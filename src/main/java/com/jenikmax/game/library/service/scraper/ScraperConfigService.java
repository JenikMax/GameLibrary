package com.jenikmax.game.library.service.scraper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

@Service
public class ScraperConfigService {

    private static final Logger log = LoggerFactory.getLogger(ScraperConfigService.class);
    private static final String CONFIG_FILE = "scrapers-config.json";

    private final ObjectMapper mapper;
    private final ConfigEncryptionService encryptionService;
    private final String configDir;

    private final Map<String, ScraperConfig> configs = new ConcurrentHashMap<>();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private Thread watchThread;
    private volatile boolean running = true;

    public ScraperConfigService(
            ObjectMapper mapper,
            ConfigEncryptionService encryptionService,
            @Value("${game-library.scraper.config-dir:#{systemProperties['user.dir'] + '/config'}}") String configDir) {
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.encryptionService = encryptionService;
        this.configDir = configDir;
    }

    @PostConstruct
    void init() {
        load();
        startWatchService();
    }

    @PreDestroy
    void destroy() {
        running = false;
        if (watchThread != null) watchThread.interrupt();
    }

    public synchronized void load() {
        File dir = new File(configDir);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, CONFIG_FILE);
        if (!file.exists()) {
            log.warn("{} not found, creating default config", file.getAbsolutePath());
            createDefaultConfig(file);
        }

        try {
            List<ScraperConfig> list = mapper.readValue(file, new TypeReference<List<ScraperConfig>>() {});
            configs.clear();
            for (ScraperConfig c : list) {
                if (c.getType() != null) {
                    configs.put(c.getType(), c);
                }
            }
            dirty.set(false);
            log.info("Loaded {} scraper configs from {}", configs.size(), file.getAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to load scraper config from {}", file.getAbsolutePath(), e);
            if (configs.isEmpty()) {
                createDefaultConfig(file);
            }
        }
    }

    public synchronized void save() {
        File dir = new File(configDir);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, CONFIG_FILE);
        try {
            List<ScraperConfig> list = new ArrayList<>(configs.values());
            mapper.writeValue(file, list);
            log.info("Saved {} scraper configs to {}", list.size(), file.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save scraper config", e);
        }
    }

    public synchronized void reload() {
        load();
    }

    public ScraperConfig getConfig(String type) {
        if (dirty.get()) reload();
        return configs.get(type);
    }

    public Map<String, ScraperConfig> getAllConfigs() {
        if (dirty.get()) reload();
        return Collections.unmodifiableMap(configs);
    }

    public List<ScraperConfig> getEnabledConfigs() {
        return getAllConfigs().values().stream()
                .filter(ScraperConfig::isEnabled)
                .collect(Collectors.toList());
    }

    public boolean isEnabled(String type) {
        ScraperConfig c = getConfig(type);
        return c != null && c.isEnabled();
    }

    public synchronized void updateConfig(String type, ScraperConfig newConfig) {
        newConfig.setType(type);
        if (newConfig.getEncryptedApiKey() != null && !newConfig.getEncryptedApiKey().isEmpty()) {
            if (!encryptionService.isEncrypted(newConfig.getEncryptedApiKey())) {
                newConfig.setEncryptedApiKey(encryptionService.encrypt(newConfig.getEncryptedApiKey()));
            }
        }
        configs.put(type, newConfig);
        save();
    }

    public String resolveApiKey(String type) {
        ScraperConfig cfg = getConfig(type);
        if (cfg == null) return null;
        String stored = cfg.getEncryptedApiKey();
        if (stored != null && !stored.isEmpty()) {
            String decrypted = encryptionService.decrypt(stored);
            if (!decrypted.equals(stored)) return decrypted;
        }
        String envKey = System.getenv("SCRAPER_" + type.toUpperCase() + "_API_KEY");
        if (envKey != null && !envKey.isEmpty()) return envKey;
        return stored;
    }

    private void startWatchService() {
        watchThread = new Thread(() -> {
            try {
                File dir = new File(configDir);
                if (!dir.exists()) dir.mkdirs();
                WatchService watcher = FileSystems.getDefault().newWatchService();
                Path path = dir.toPath();
                path.register(watcher, ENTRY_CREATE, ENTRY_MODIFY);
                while (running) {
                    WatchKey key = watcher.poll(5000, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (key != null) {
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.context().toString().equals(CONFIG_FILE)) {
                                dirty.set(true);
                                log.info("Config file changed on disk, will reload on next access");
                            }
                        }
                        key.reset();
                    }
                }
                watcher.close();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.warn("WatchService not available, config auto-reload disabled", e);
            }
        }, "scraper-config-watcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void createDefaultConfig(File file) {
        List<ScraperConfig> defaults = buildDefaultConfigs();
        try {
            mapper.writeValue(file, defaults);
            for (ScraperConfig c : defaults) {
                configs.put(c.getType(), c);
            }
            dirty.set(false);
            log.info("Created default config with {} scrapers", defaults.size());
        } catch (IOException e) {
            log.error("Failed to create default config", e);
        }
    }

    private List<ScraperConfig> buildDefaultConfigs() {
        List<ScraperConfig> list = new ArrayList<>();

        ScraperConfig playground = new ScraperConfig();
        playground.setType("playground");
        playground.setEnabled(true);
        playground.setDisplayName("Playground");
        playground.setBaseUrl("https://playground.ru");
        playground.setSslProtocol("TLSv1.2");
        playground.setCssSelectors(new LinkedHashMap<>());
        playground.getCssSelectors().put("screenshotItem", "a[href*=\"/i/screenshot/\"]");
        playground.setGenreMappings(new LinkedHashMap<>());
        playground.getGenreMappings().put("action", Arrays.asList("action"));
        playground.getGenreMappings().put("adventure", Arrays.asList("adventure"));
        playground.getGenreMappings().put("rpg", Arrays.asList("rpg"));
        playground.getGenreMappings().put("first-person", Arrays.asList("first_person"));
        playground.getGenreMappings().put("third-person", Arrays.asList("third_person"));
        playground.getGenreMappings().put("open-world", Arrays.asList("open_world"));
        playground.getGenreMappings().put("post-apocalyptic", Arrays.asList("post_apocalyptic"));
        list.add(playground);

        ScraperConfig igromania = new ScraperConfig();
        igromania.setType("igromania");
        igromania.setEnabled(true);
        igromania.setDisplayName("Igromania");
        igromania.setBaseUrl("https://www.igromania.ru");
        igromania.setSslProtocol("TLSv1.2");
        igromania.setJsonPaths(new LinkedHashMap<>());
        igromania.getJsonPaths().put("title", "$.props.initialStoreState.databaseElementPageStore.element.name");
        igromania.getJsonPaths().put("description", "$.props.initialStoreState.databaseElementPageStore.element.description");
        igromania.getJsonPaths().put("year", "$.props.initialStoreState.databaseElementPageStore.element.release_date.string");
        igromania.getJsonPaths().put("posterImage", "$.props.initialStoreState.databaseElementPageStore.element.image.origin");
        igromania.getJsonPaths().put("genres", "$.props.initialStoreState.databaseElementPageStore.element.genres[*].slug");
        igromania.getJsonPaths().put("screenshots", "$.props.initialStoreState.databaseElementPageStore.screenshots.items.results[*].file.origin");
        igromania.setGenreMappings(buildDefaultGenreMappings());
        list.add(igromania);

        ScraperConfig steam = new ScraperConfig();
        steam.setType("steam");
        steam.setEnabled(false);
        steam.setDisplayName("Steam");
        steam.setApiUrl("https://api.steampowered.com/ISteamApps/GetAppDetails/v1/");
        list.add(steam);

        ScraperConfig mobygames = new ScraperConfig();
        mobygames.setType("mobygames");
        mobygames.setEnabled(false);
        mobygames.setDisplayName("MobyGames");
        mobygames.setApiUrl("https://api.mobygames.com/v1/games");
        list.add(mobygames);

        ScraperConfig igdb = new ScraperConfig();
        igdb.setType("igdb");
        igdb.setEnabled(false);
        igdb.setDisplayName("IGDB");
        igdb.setApiUrl("https://api.igdb.com/v4/games");
        igdb.setAuthScheme("Bearer");
        igdb.setHeaders(new LinkedHashMap<>());
        igdb.getHeaders().put("Client-ID", "");
        list.add(igdb);

        ScraperConfig thegamesdb = new ScraperConfig();
        thegamesdb.setType("thegamesdb");
        thegamesdb.setEnabled(false);
        thegamesdb.setDisplayName("TheGamesDB");
        thegamesdb.setApiUrl("https://api.thegamesdb.net/v1/Games/ByGameName");
        list.add(thegamesdb);

        ScraperConfig worldart = new ScraperConfig();
        worldart.setType("worldart");
        worldart.setEnabled(false);
        worldart.setDisplayName("World-Art");
        worldart.setBaseUrl("http://www.world-art.ru");
        worldart.setSslProtocol("TLSv1.2");
        worldart.setCssSelectors(new LinkedHashMap<>());
        worldart.getCssSelectors().put("searchForm", "form#searchform");
        worldart.getCssSelectors().put("searchInput", "input[name='searchtext']");
        worldart.getCssSelectors().put("searchSubmit", "input[name='dosearch']");
        worldart.getCssSelectors().put("resultItem", ".foundresult");
        worldart.getCssSelectors().put("resultTitle", ".RUbreaking");
        list.add(worldart);

        return list;
    }

    private Map<String, List<String>> buildDefaultGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("beat-em-up", Arrays.asList("beat_em_up"));
        map.put("soulslike", Arrays.asList("soulslike"));
        map.put("rolevoj-ekshen", Arrays.asList("action", "rpg"));
        map.put("fps", Arrays.asList("first_person", "shooter"));
        map.put("hack-and-slash", Arrays.asList("hack_and_slash"));
        map.put("immersive-sim", Arrays.asList("immersive_sim"));
        map.put("mmo", Arrays.asList("mmo"));
        map.put("moba", Arrays.asList("moba"));
        map.put("point-click", Arrays.asList("point_click"));
        map.put("roguelike", Arrays.asList("roguelike"));
        map.put("td", Arrays.asList("tower_defence"));
        map.put("tps", Arrays.asList("third_person", "shooter"));
        map.put("arkada", Arrays.asList("arcade"));
        map.put("vizualnaia-novella", Arrays.asList("vizualnaia_novella"));
        map.put("vyzhivanie", Arrays.asList("survival"));
        map.put("gonki", Arrays.asList("racing"));
        map.put("igra-dlia-vzroslykh", Arrays.asList("adult"));
        map.put("interaktivnoe-kino", Arrays.asList("interaktivnoe_kino"));
        map.put("istoriia", Arrays.asList("istoriia"));
        map.put("kvest", Arrays.asList("quest"));
        map.put("kki", Arrays.asList("kki"));
        map.put("kooperativ", Arrays.asList("coop"));
        map.put("menedzher", Arrays.asList("manager"));
        map.put("metroidvaniia", Arrays.asList("metroidvaniia"));
        map.put("muzyka", Arrays.asList("music"));
        map.put("nastolnaia-igra", Arrays.asList("board_game"));
        map.put("pazzl", Arrays.asList("pazzl"));
        map.put("platformer", Arrays.asList("platform"));
        map.put("poshagovaia-strategiia", Arrays.asList("strategy", "turn_based"));
        map.put("prikliuchenie", Arrays.asList("adventure"));
        map.put("rolevaia-igra", Arrays.asList("rpg"));
        map.put("simuliator", Arrays.asList("simulators"));
        map.put("simuliator-khodby", Arrays.asList("other_simulators"));
        map.put("sport", Arrays.asList("sport"));
        map.put("strategiia", Arrays.asList("strategy"));
        map.put("strategiia-v-realnom-vremeni", Arrays.asList("rts"));
        map.put("taktika", Arrays.asList("tactics"));
        map.put("faiting", Arrays.asList("fighting"));
        map.put("khorror", Arrays.asList("horror"));
        map.put("shuter", Arrays.asList("shooter"));
        map.put("ekshen", Arrays.asList("action"));
        return map;
    }
}

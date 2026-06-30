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
import org.springframework.context.annotation.Lazy;
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
    private final ScraperFactory scraperFactory;

    public ScraperConfigService(
            ObjectMapper mapper,
            ConfigEncryptionService encryptionService,
            @Lazy ScraperFactory scraperFactory,
            @Value("${game-library.scraper.config-dir:#{systemProperties['user.dir'] + '/config'}}") String configDir) {
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
        this.encryptionService = encryptionService;
        this.scraperFactory = scraperFactory;
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
        ScraperConfig existing = configs.get(type);

        String incomingKey = newConfig.getEncryptedApiKey();
        if (incomingKey == null || incomingKey.isEmpty() || incomingKey.contains("****")) {
            if (existing != null) {
                newConfig.setEncryptedApiKey(existing.getEncryptedApiKey());
            }
        } else if (!encryptionService.isEncrypted(incomingKey)) {
            newConfig.setEncryptedApiKey(encryptionService.encrypt(incomingKey));
        }
        configs.put(type, newConfig);
        save();
        scraperFactory.invalidateCache();
    }

    public String resolveApiKey(String type) {
        ScraperConfig cfg = getConfig(type);
        if (cfg == null) return null;
        String stored = cfg.getEncryptedApiKey();
        if (stored != null && !stored.isEmpty()) {
            String decrypted = encryptionService.decrypt(stored);
            if (decrypted != null && !decrypted.equals(stored)) return decrypted;
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
        playground.getGenreMappings().putAll(buildWorldArtGenreMappings());
        playground.getGenreMappings().put("рогалик", Arrays.asList("roguelike"));
        playground.getGenreMappings().put("глобальная_стратегия", Arrays.asList("_4X"));
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
        steam.setEnabled(true);
        steam.setDisplayName("Steam");
        steam.setApiUrl("https://store.steampowered.com/api/appdetails");
        steam.setTimeoutMs(10000);
        steam.setMaxScreenshots(20);
        steam.setGenreMappings(buildSteamGenreMappings());
        list.add(steam);

        ScraperConfig igdb = new ScraperConfig();
        igdb.setType("igdb");
        igdb.setEnabled(true);
        igdb.setDisplayName("IGDB");
        igdb.setApiUrl("https://api.igdb.com/v4/games");
        igdb.setAuthScheme("Bearer");
        igdb.setHeaders(new LinkedHashMap<>());
        igdb.getHeaders().put("Client-ID", "");
        igdb.setGenreMappings(buildIgdbGenreMappings());
        list.add(igdb);

        ScraperConfig thegamesdb = new ScraperConfig();
        thegamesdb.setType("thegamesdb");
        thegamesdb.setEnabled(true);
        thegamesdb.setDisplayName("TheGamesDB");
        thegamesdb.setApiUrl("https://api.thegamesdb.net/v1/Games/ByGameName");
        thegamesdb.setGenreMappings(buildTheGameDBGenreMappings());
        list.add(thegamesdb);

        ScraperConfig worldart = new ScraperConfig();
        worldart.setType("worldart");
        worldart.setEnabled(true);
        worldart.setDisplayName("World-Art");
        worldart.setBaseUrl("http://www.world-art.ru");
        worldart.setSslProtocol("TLSv1.2");
        worldart.setGenreMappings(buildWorldArtGenreMappings());
        list.add(worldart);

        ScraperConfig psxdc = new ScraperConfig();
        psxdc.setType("psxdatacenter");
        psxdc.setEnabled(true);
        psxdc.setDisplayName("PSX DataCenter");
        psxdc.setBaseUrl("https://psxdatacenter.com");
        psxdc.setMaxScreenshots(10);
        psxdc.setGenreMappings(buildPsxDataCenterGenreMappings());
        list.add(psxdc);

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
        map.put("rouglike", Arrays.asList("roguelike"));
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

    private Map<String, List<String>> buildWorldArtGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("файтинг", Arrays.asList("fighting"));
        map.put("приключенческий экшн", Arrays.asList("action", "adventure"));
        map.put("платформер", Arrays.asList("platform"));
        map.put("стелс", Arrays.asList("stealth"));
        map.put("слэшер", Arrays.asList("slasher"));
        map.put("открытый мир", Arrays.asList("open_world"));
        map.put("выживание", Arrays.asList("survival"));
        map.put("головоломка", Arrays.asList("pazzl"));
        map.put("ролевая игра", Arrays.asList("rpg"));
        map.put("ммо", Arrays.asList("mmo"));
        map.put("социальные видеоигры", Arrays.asList("other"));
        map.put("музыкальная игра", Arrays.asList("music"));
        map.put("метроидвания", Arrays.asList("metroidvaniia"));
        map.put("битемап", Arrays.asList("beat_em_up"));
        map.put("роглайк", Arrays.asList("roguelike"));
        map.put("королевская битва", Arrays.asList("battle_royale"));
        map.put("песочница", Arrays.asList("sandbox"));
        map.put("qte", Arrays.asList("other"));
        map.put("исследование подземелий", Arrays.asList("dungeons"));
        map.put("декбилдинг", Arrays.asList("other"));
        map.put("автобаттлер", Arrays.asList("other"));
        map.put("фермерство", Arrays.asList("other_simulators"));
        map.put("обустройство лагеря", Arrays.asList("other_simulators"));
        map.put("командный файтинг", Arrays.asList("fighting"));
        map.put("асимметричный мультиплеер", Arrays.asList("online"));
        map.put("лутер", Arrays.asList("action", "rpg"));
        map.put("автоскроллер", Arrays.asList("arcade"));
        map.put("битвы с боссами", Arrays.asList("other"));
        map.put("уровни сложности", Arrays.asList("other"));
        map.put("сложная", Arrays.asList("other"));
        map.put("крафтинг", Arrays.asList("other"));
        map.put("дерево умений", Arrays.asList("rpg"));
        map.put("на время", Arrays.asList("other"));
        map.put("мини-игры", Arrays.asList("other"));
        map.put("idle", Arrays.asList("other_simulators"));
        map.put("бои с парированием", Arrays.asList("action", "fighting"));
        map.put("паркур", Arrays.asList("action"));
        map.put("стимпанк", Arrays.asList("steampunk"));
        map.put("постапокалиптический мир", Arrays.asList("post_apocalyptic"));
        map.put("современный мир", Arrays.asList("other"));
        map.put("фантастика", Arrays.asList("sci_fi"));
        map.put("историческая", Arrays.asList("istoriia"));
        map.put("фэнтези", Arrays.asList("fantasy"));
        map.put("киберпанк", Arrays.asList("cyberpunk"));
        map.put("хоррор / триллер", Arrays.asList("horror"));
        map.put("вестерн", Arrays.asList("western"));
        map.put("дизельпанк", Arrays.asList("steampunk", "post_apocalyptic"));
        map.put("зомби", Arrays.asList("zombie"));
        map.put("вампиры", Arrays.asList("fantasy", "horror"));
        map.put("будущее", Arrays.asList("sci_fi"));
        map.put("антиутопия", Arrays.asList("sci_fi", "cyberpunk"));
        map.put("технофэнтези", Arrays.asList("fantasy", "sci_fi"));
        map.put("путешествие в другой мир", Arrays.asList("adventure"));
        map.put("кроссовер", Arrays.asList("other"));
        map.put("динозавры", Arrays.asList("other"));
        map.put("нуар", Arrays.asList("other"));
        map.put("ниндзя", Arrays.asList("stealth", "action"));
        map.put("морские пираты", Arrays.asList("adventure"));
        map.put("путешествие во времени", Arrays.asList("sci_fi"));
        map.put("подводный мир", Arrays.asList("other"));
        map.put("школа / университет", Arrays.asList("other"));
        map.put("инопланетяне", Arrays.asList("sci_fi"));
        map.put("оборотни", Arrays.asList("fantasy", "horror"));
        map.put("драконы", Arrays.asList("fantasy"));
        map.put("альтернативная история", Arrays.asList("istoriia"));
        map.put("вымышленный мир", Arrays.asList("fantasy"));
        map.put("биопанк", Arrays.asList("cyberpunk", "sci_fi"));
        map.put("ангелы", Arrays.asList("fantasy"));
        map.put("демоны", Arrays.asList("fantasy", "horror"));
        map.put("греческая мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("славянская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("египетская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("скандинавская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("китайская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("кельтская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("североамериканская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("японская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("финно-угорская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("якутская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("арабская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("индонезийская мифология", Arrays.asList("fantasy", "istoriia"));
        map.put("чёрно-белая", Arrays.asList("other"));
        map.put("пиксельная графика", Arrays.asList("retro"));
        map.put("жестокие сцены", Arrays.asList("horror"));
        map.put("рисованная графика", Arrays.asList("other"));
        map.put("cел-шейдинг", Arrays.asList("anime"));
        map.put("воксельная графика", Arrays.asList("other"));
        map.put("мужской протагонист", Arrays.asList("other"));
        map.put("женский протагонист", Arrays.asList("other"));
        map.put("выбор героя в начале игры", Arrays.asList("rpg"));
        map.put("несколько героев", Arrays.asList("other"));
        map.put("зафиксированный экран", Arrays.asList("other"));
        map.put("изометрическая", Arrays.asList("top_down"));
        map.put("от первого лица", Arrays.asList("first_person"));
        map.put("от третьего лица", Arrays.asList("third_person"));
        map.put("3d", Arrays.asList("other"));
        map.put("вид сбоку", Arrays.asList("platform"));
        map.put("2.5d", Arrays.asList("other"));
        map.put("тактическая", Arrays.asList("tactics"));
        map.put("стратегия", Arrays.asList("strategy"));
        map.put("симулятор бизнеса", Arrays.asList("manager", "economy"));
        map.put("градостроительство", Arrays.asList("construction", "strategy"));
        map.put("симулятор бога", Arrays.asList("simulators", "strategy"));
        map.put("защита башни", Arrays.asList("tower_defence"));
        map.put("захват флага", Arrays.asList("action"));
        map.put("моба", Arrays.asList("moba"));
        map.put("пошаговые сражения", Arrays.asList("turn_based"));
        map.put("4x", Arrays.asList("_4X"));
        map.put("игровое поле с гексам", Arrays.asList("strategy", "turn_based"));
        map.put("игровое поле с клеткам", Arrays.asList("strategy", "turn_based"));
        map.put("автоматизация процесса", Arrays.asList("economy"));
        map.put("шутер", Arrays.asList("shooter"));
        map.put("рельсовая", Arrays.asList("shooter", "arcade"));
        map.put("беги и стреляй", Arrays.asList("shooter", "action"));
        map.put("кьютемап", Arrays.asList("other"));
        map.put("шутемап", Arrays.asList("shooter"));
        map.put("аркадные гонки", Arrays.asList("racing", "arcade"));
        map.put("битвы машин", Arrays.asList("racing", "action"));
        map.put("формула-1", Arrays.asList("racing"));
        map.put("карт", Arrays.asList("racing"));
        map.put("мотоспорт", Arrays.asList("moto", "racing"));
        map.put("бигфуты", Arrays.asList("racing"));
        map.put("ралли-бездорожье", Arrays.asList("racing"));
        map.put("гоночный симулятор", Arrays.asList("racing", "simulators"));
        map.put("эротика / для взрослых", Arrays.asList("adult"));
        map.put("текстовый квест", Arrays.asList("quest"));
        map.put("интерактивное кино", Arrays.asList("interaktivnoe_kino"));
        map.put("приключенческий квест", Arrays.asList("quest", "adventure"));
        map.put("визуальная новелла", Arrays.asList("vizualnaia_novella"));
        map.put("хентайные сцены", Arrays.asList("adult"));
        map.put("отомэ", Arrays.asList("vizualnaia_novella"));
        map.put("глубокий сюжет", Arrays.asList("other"));
        map.put("исследование", Arrays.asList("adventure"));
        map.put("point-and-click", Arrays.asList("point_click"));
        map.put("решения с последствиями", Arrays.asList("rpg"));
        map.put("кинетическая новелла", Arrays.asList("vizualnaia_novella"));
        map.put("несколько концовок", Arrays.asList("rpg"));
        map.put("симулятор свиданий", Arrays.asList("adult", "simulators"));
        map.put("расследование", Arrays.asList("quest"));
        map.put("карточная игра", Arrays.asList("card"));
        map.put("шахматы", Arrays.asList("board_game", "strategy"));
        map.put("бильярд", Arrays.asList("sport"));
        map.put("го", Arrays.asList("board_game", "strategy"));
        map.put("маджонг", Arrays.asList("board_game", "pazzl"));
        map.put("пинбол", Arrays.asList("arcade"));
        map.put("настольная игра", Arrays.asList("board_game"));
        map.put("сёги", Arrays.asList("board_game", "strategy"));
        map.put("кроссворды", Arrays.asList("pazzl"));
        map.put("обучающая игра", Arrays.asList("other"));
        map.put("поиск предметов", Arrays.asList("quest"));
        map.put("найди отличие", Arrays.asList("pazzl"));
        map.put("судоку", Arrays.asList("pazzl", "logic"));
        map.put("игровое шоу", Arrays.asList("other"));
        map.put("азартные игры", Arrays.asList("other"));
        map.put("три в ряд", Arrays.asList("pazzl"));
        map.put("сельхозтехника", Arrays.asList("simulators", "other_simulators"));
        map.put("гражданская авиация", Arrays.asList("flight", "simulators"));
        map.put("военная авиация", Arrays.asList("flight", "action"));
        map.put("вертолёты", Arrays.asList("flight", "simulators"));
        map.put("меха", Arrays.asList("meha"));
        map.put("космические корабли", Arrays.asList("space", "simulators"));
        map.put("подводные лодки", Arrays.asList("simulators"));
        map.put("танки", Arrays.asList("action", "simulators"));
        map.put("поезда", Arrays.asList("simulators"));
        map.put("грузовой транспорт", Arrays.asList("simulators"));
        map.put("корабли", Arrays.asList("simulators"));
        map.put("няньки", Arrays.asList("simulators"));
        map.put("кулинарии", Arrays.asList("simulators"));
        map.put("дизайнера", Arrays.asList("simulators"));
        map.put("садовода", Arrays.asList("simulators", "other_simulators"));
        map.put("врача", Arrays.asList("simulators"));
        map.put("виртуальной жизни", Arrays.asList("simulators"));
        map.put("заботы о животных", Arrays.asList("simulators"));
        map.put("пожарного", Arrays.asList("simulators", "other_simulators"));
        map.put("строителя / рабочего", Arrays.asList("construction", "simulators"));
        map.put("американский футбол", Arrays.asList("sport"));
        map.put("бейсбол", Arrays.asList("sport"));
        map.put("баскетбол", Arrays.asList("basketball", "sport"));
        map.put("вело", Arrays.asList("sport", "racing"));
        map.put("боулинг", Arrays.asList("sport"));
        map.put("бокс", Arrays.asList("fighting", "sport"));
        map.put("охота и рыбалка", Arrays.asList("sport", "simulators"));
        map.put("фитнес", Arrays.asList("sport"));
        map.put("футбол", Arrays.asList("football", "sport"));
        map.put("гольф", Arrays.asList("sport"));
        map.put("конный", Arrays.asList("sport"));
        map.put("хоккей", Arrays.asList("hockey", "sport"));
        map.put("разное", Arrays.asList("sport"));
        map.put("регби", Arrays.asList("sport"));
        map.put("скейтинг", Arrays.asList("sport"));
        map.put("прыжки с парашютом", Arrays.asList("sport"));
        map.put("сноуборд и лыжи", Arrays.asList("sport"));
        map.put("сёрфинг", Arrays.asList("sport"));
        map.put("теннис", Arrays.asList("sport"));
        map.put("плавание", Arrays.asList("sport"));
        map.put("волейбол", Arrays.asList("sport"));
        map.put("реслинг", Arrays.asList("fighting", "sport"));
        map.put("трассировка лучей", Arrays.asList("other"));
        map.put("световой пистолет", Arrays.asList("shooter", "arcade"));
        map.put("kinect", Arrays.asList("other"));
        map.put("playstation move", Arrays.asList("other"));
        map.put("vr", Arrays.asList("vr"));
        map.put("toys-to-life", Arrays.asList("other"));
        map.put("разрушаемое окружение", Arrays.asList("action"));
        map.put("физика", Arrays.asList("other"));
        map.put("пререндерная графика", Arrays.asList("other"));
        map.put("фанфикшн", Arrays.asList("other"));
        map.put("игра-реклама", Arrays.asList("other"));
        map.put("бесплатная игра", Arrays.asList("free_to_play"));
        map.put("краудфандинг", Arrays.asList("indie"));
        map.put("с ранним доступом", Arrays.asList("other"));
        map.put("лутбоксы", Arrays.asList("other"));
        map.put("боевой пропуск", Arrays.asList("other"));
        map.put("порт мобильной игры", Arrays.asList("other"));
        map.put("порт аркадной игры", Arrays.asList("arcade"));
        map.put("эпизодическая", Arrays.asList("other"));
        map.put("микротранзакции", Arrays.asList("other"));
        map.put("по мотивам аниме/манги", Arrays.asList("anime"));
        map.put("по мотивам фильма", Arrays.asList("other"));
        map.put("по мотивам книги", Arrays.asList("other"));
        map.put("по мотивам комикса", Arrays.asList("other"));
        map.put("по мотивам мультфильма", Arrays.asList("anime"));
        map.put("по мотивам телепередачи", Arrays.asList("other"));
        return map;
    }

    private Map<String, List<String>> buildSteamGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Action", Arrays.asList("action"));
        map.put("Adventure", Arrays.asList("adventure"));
        map.put("Casual", Arrays.asList("casual"));
        map.put("Indie", Arrays.asList("indie"));
        map.put("Massively Multiplayer", Arrays.asList("mmo"));
        map.put("Racing", Arrays.asList("racing"));
        map.put("RPG", Arrays.asList("rpg"));
        map.put("Simulation", Arrays.asList("simulators"));
        map.put("Sports", Arrays.asList("sport"));
        map.put("Strategy", Arrays.asList("strategy"));
        map.put("Early Access", Arrays.asList("other"));
        map.put("Free to Play", Arrays.asList("free_to_play"));
        map.put("Violent", Arrays.asList("other"));
        map.put("Gore", Arrays.asList("horror"));
        map.put("Sexual Content", Arrays.asList("adult"));
        map.put("Nudity", Arrays.asList("adult"));
        map.put("Action RPG", Arrays.asList("action", "rpg"));
        map.put("Action-Adventure", Arrays.asList("action", "adventure"));
        map.put("Addictive", Arrays.asList("other"));
        map.put("Arcade", Arrays.asList("arcade"));
        map.put("Battle Royale", Arrays.asList("battle_royale"));
        map.put("Beat 'em up", Arrays.asList("beat_em_up"));
        map.put("Board Game", Arrays.asList("board_game"));
        map.put("Building", Arrays.asList("construction"));
        map.put("Bullet Hell", Arrays.asList("shooter", "arcade"));
        map.put("Card Game", Arrays.asList("card"));
        map.put("Cartoon", Arrays.asList("other"));
        map.put("Cartoony", Arrays.asList("other"));
        map.put("Character Customization", Arrays.asList("other"));
        map.put("City Builder", Arrays.asList("construction", "strategy"));
        map.put("Choices Matter", Arrays.asList("rpg"));
        map.put("Classic", Arrays.asList("other"));
        map.put("Co-op", Arrays.asList("coop"));
        map.put("Collectible Card Game", Arrays.asList("card"));
        map.put("Colorful", Arrays.asList("other"));
        map.put("Comedy", Arrays.asList("other"));
        map.put("Competitive", Arrays.asList("online"));
        map.put("Crafting", Arrays.asList("other"));
        map.put("Crime", Arrays.asList("action"));
        map.put("Cute", Arrays.asList("other"));
        map.put("Cycling", Arrays.asList("racing"));
        map.put("Dark", Arrays.asList("horror"));
        map.put("Dark Comedy", Arrays.asList("other"));
        map.put("Dark Fantasy", Arrays.asList("fantasy", "horror"));
        map.put("Dating Sim", Arrays.asList("adult", "simulators"));
        map.put("Demons", Arrays.asList("fantasy", "horror"));
        map.put("Design & Illustration", Arrays.asList("other"));
        map.put("Detective", Arrays.asList("quest"));
        map.put("Difficult", Arrays.asList("other"));
        map.put("Dinosaurs", Arrays.asList("other"));
        map.put("Dragons", Arrays.asList("fantasy"));
        map.put("Drama", Arrays.asList("other"));
        map.put("Driving", Arrays.asList("racing"));
        map.put("Dungeon Crawler", Arrays.asList("dungeons", "rpg"));
        map.put("Economy", Arrays.asList("economy", "manager"));
        map.put("Education", Arrays.asList("other"));
        map.put("Emotional", Arrays.asList("other"));
        map.put("Episodic", Arrays.asList("other"));
        map.put("Experimental", Arrays.asList("other"));
        map.put("Exploration", Arrays.asList("adventure"));
        map.put("Fantasy", Arrays.asList("fantasy"));
        map.put("Fast-Paced", Arrays.asList("action"));
        map.put("Fighting", Arrays.asList("fighting"));
        map.put("First-Person", Arrays.asList("first_person"));
        map.put("Flight", Arrays.asList("flight"));
        map.put("FMV", Arrays.asList("interaktivnoe_kino"));
        map.put("Football", Arrays.asList("football", "sport"));
        map.put("Funny", Arrays.asList("other"));
        map.put("Golf", Arrays.asList("sport"));
        map.put("Grand Strategy", Arrays.asList("strategy", "_4X"));
        map.put("Great Soundtrack", Arrays.asList("other"));
        map.put("Gun Customization", Arrays.asList("shooter"));
        map.put("Hack and Slash", Arrays.asList("hack_and_slash"));
        map.put("Heist", Arrays.asList("action"));
        map.put("Hidden Object", Arrays.asList("quest", "pazzl"));
        map.put("Historical", Arrays.asList("istoriia"));
        map.put("Horror", Arrays.asList("horror"));
        map.put("Horses", Arrays.asList("sport"));
        map.put("Hunting", Arrays.asList("sport"));
        map.put("Idler", Arrays.asList("other_simulators"));
        map.put("Immersive Sim", Arrays.asList("immersive_sim"));
        map.put("Interactive Fiction", Arrays.asList("quest"));
        map.put("Inventory Management", Arrays.asList("manager"));
        map.put("Investigation", Arrays.asList("quest"));
        map.put("Isometric", Arrays.asList("top_down"));
        map.put("JRPG", Arrays.asList("rpg"));
        map.put("LEGO", Arrays.asList("other"));
        map.put("Life Simulation", Arrays.asList("simulators"));
        map.put("Linear", Arrays.asList("other"));
        map.put("Local Co-Op", Arrays.asList("coop"));
        map.put("Local Multiplayer", Arrays.asList("online"));
        map.put("Logic", Arrays.asList("pazzl", "logic"));
        map.put("Loot", Arrays.asList("rpg"));
        map.put("Looter Shooter", Arrays.asList("shooter", "rpg"));
        map.put("Lovecraftian", Arrays.asList("horror"));
        map.put("Management", Arrays.asList("manager"));
        map.put("Martial Arts", Arrays.asList("fighting"));
        map.put("Mature", Arrays.asList("other"));
        map.put("Medieval", Arrays.asList("fantasy", "istoriia"));
        map.put("Memes", Arrays.asList("other"));
        map.put("Metroidvania", Arrays.asList("metroidvaniia"));
        map.put("Military", Arrays.asList("action"));
        map.put("Mini Golf", Arrays.asList("sport"));
        map.put("Minimalist", Arrays.asList("other"));
        map.put("MOBA", Arrays.asList("moba"));
        map.put("Moddable", Arrays.asList("other"));
        map.put("Modern", Arrays.asList("other"));
        map.put("Motocross", Arrays.asList("moto", "racing"));
        map.put("Motorbike", Arrays.asList("moto", "racing"));
        map.put("Mouse only", Arrays.asList("other"));
        map.put("Multiplayer", Arrays.asList("online"));
        map.put("Multiple Endings", Arrays.asList("rpg"));
        map.put("Music", Arrays.asList("music"));
        map.put("Mystery", Arrays.asList("quest"));
        map.put("Mythology", Arrays.asList("fantasy", "istoriia"));
        map.put("Nature", Arrays.asList("other"));
        map.put("Naval", Arrays.asList("simulators"));
        map.put("Narration", Arrays.asList("other"));
        map.put("Noir", Arrays.asList("other"));
        map.put("Nonlinear", Arrays.asList("other"));
        map.put("Open World", Arrays.asList("open_world"));
        map.put("Otome", Arrays.asList("vizualnaia_novella"));
        map.put("Parkour", Arrays.asList("action"));
        map.put("Parody", Arrays.asList("other"));
        map.put("Party", Arrays.asList("other"));
        map.put("Party-Based RPG", Arrays.asList("rpg"));
        map.put("Perma Death", Arrays.asList("roguelike"));
        map.put("Photo Editing", Arrays.asList("other"));
        map.put("Physics", Arrays.asList("other"));
        map.put("Pinball", Arrays.asList("arcade"));
        map.put("Pixel Graphics", Arrays.asList("retro"));
        map.put("Platformer", Arrays.asList("platform"));
        map.put("Point & Click", Arrays.asList("point_click"));
        map.put("Post-apocalyptic", Arrays.asList("post_apocalyptic"));
        map.put("Procedural Generation", Arrays.asList("roguelike"));
        map.put("Programming", Arrays.asList("other"));
        map.put("Psychedelic", Arrays.asList("other"));
        map.put("Psychological", Arrays.asList("horror"));
        map.put("Psychological Horror", Arrays.asList("horror"));
        map.put("Puzzle", Arrays.asList("pazzl"));
        map.put("Puzzle Platformer", Arrays.asList("pazzl", "platform"));
        map.put("PvP", Arrays.asList("online"));
        map.put("Quick-Time Events", Arrays.asList("action"));
        map.put("Real-Time", Arrays.asList("action"));
        map.put("Real-Time Strategy", Arrays.asList("rts"));
        map.put("Real-Time Tactics", Arrays.asList("tactics", "rts"));
        map.put("Realistic", Arrays.asList("other"));
        map.put("Relaxing", Arrays.asList("other"));
        map.put("Remake", Arrays.asList("other"));
        map.put("Resource Management", Arrays.asList("economy", "manager"));
        map.put("Retro", Arrays.asList("retro"));
        map.put("Rhythm", Arrays.asList("music"));
        map.put("Robots", Arrays.asList("sci_fi"));
        map.put("Roguelike", Arrays.asList("roguelike"));
        map.put("Roguelite", Arrays.asList("roguelike"));
        map.put("Romance", Arrays.asList("other"));
        map.put("RPGMaker", Arrays.asList("rpg"));
        map.put("Running", Arrays.asList("sport"));
        map.put("Sandbox", Arrays.asList("sandbox"));
        map.put("Satire", Arrays.asList("other"));
        map.put("Sci-fi", Arrays.asList("sci_fi"));
        map.put("Sailing", Arrays.asList("simulators"));
        map.put("Shoot 'Em Up", Arrays.asList("shooter", "arcade"));
        map.put("Shooter", Arrays.asList("shooter"));
        map.put("Short", Arrays.asList("other"));
        map.put("Side Scroller", Arrays.asList("platform"));
        map.put("Simulation", Arrays.asList("simulators"));
        map.put("Singleplayer", Arrays.asList("other"));
        map.put("Skateboarding", Arrays.asList("sport"));
        map.put("Sniper", Arrays.asList("shooter"));
        map.put("Snow", Arrays.asList("sport"));
        map.put("Snowboarding", Arrays.asList("sport"));
        map.put("Software", Arrays.asList("other"));
        map.put("Sokoban", Arrays.asList("pazzl"));
        map.put("Souls-like", Arrays.asList("soulslike"));
        map.put("Space", Arrays.asList("space", "sci_fi"));
        map.put("Space Sim", Arrays.asList("space", "simulators"));
        map.put("Spectacle fighter", Arrays.asList("action", "fighting"));
        map.put("Speedrun", Arrays.asList("action"));
        map.put("Split Screen", Arrays.asList("coop"));
        map.put("Stealth", Arrays.asList("stealth"));
        map.put("Steam Workshop", Arrays.asList("other"));
        map.put("Steampunk", Arrays.asList("steampunk"));
        map.put("Story Rich", Arrays.asList("other"));
        map.put("Strategy", Arrays.asList("strategy"));
        map.put("Strategy RPG", Arrays.asList("strategy", "rpg"));
        map.put("Stylized", Arrays.asList("other"));
        map.put("Superhero", Arrays.asList("action"));
        map.put("Surreal", Arrays.asList("other"));
        map.put("Survival", Arrays.asList("survival"));
        map.put("Survival Horror", Arrays.asList("survival", "horror"));
        map.put("Swordplay", Arrays.asList("action", "fighting"));
        map.put("Tabletop", Arrays.asList("board_game"));
        map.put("Tactical", Arrays.asList("tactics"));
        map.put("Tactical RPG", Arrays.asList("tactics", "rpg"));
        map.put("Tanks", Arrays.asList("action", "simulators"));
        map.put("Team-Based", Arrays.asList("online"));
        map.put("Text-Based", Arrays.asList("quest"));
        map.put("Third Person", Arrays.asList("third_person"));
        map.put("Third-Person Shooter", Arrays.asList("third_person", "shooter"));
        map.put("Thriller", Arrays.asList("horror"));
        map.put("Top-Down", Arrays.asList("top_down"));
        map.put("Top-Down Shooter", Arrays.asList("top_down", "shooter"));
        map.put("Tower Defense", Arrays.asList("tower_defence"));
        map.put("TrackIR", Arrays.asList("other"));
        map.put("Trading", Arrays.asList("economy"));
        map.put("Trading Card Game", Arrays.asList("card"));
        map.put("Trivia", Arrays.asList("other"));
        map.put("Turn-Based", Arrays.asList("turn_based"));
        map.put("Turn-Based RPG", Arrays.asList("turn_based", "rpg"));
        map.put("Turn-Based Strategy", Arrays.asList("turn_based", "strategy"));
        map.put("Tutorial", Arrays.asList("other"));
        map.put("Twin Stick Shooter", Arrays.asList("shooter", "arcade"));
        map.put("Tycoon", Arrays.asList("manager", "economy"));
        map.put("Underwater", Arrays.asList("other"));
        map.put("Utilities", Arrays.asList("other"));
        map.put("VR", Arrays.asList("vr"));
        map.put("Villain Protagonist", Arrays.asList("other"));
        map.put("Violent", Arrays.asList("other"));
        map.put("Visual Novel", Arrays.asList("vizualnaia_novella"));
        map.put("Voice Control", Arrays.asList("other"));
        map.put("Walking Simulator", Arrays.asList("other"));
        map.put("War", Arrays.asList("action"));
        map.put("Wargame", Arrays.asList("strategy"));
        map.put("Warhammer 40K", Arrays.asList("sci_fi"));
        map.put("Wholesome", Arrays.asList("other"));
        map.put("Word Game", Arrays.asList("pazzl"));
        map.put("World War I", Arrays.asList("istoriia"));
        map.put("World War II", Arrays.asList("istoriia"));
        map.put("Wrestling", Arrays.asList("fighting", "sport"));
        map.put("Zombies", Arrays.asList("zombie", "horror"));
        return map;
    }

    private Map<String, List<String>> buildIgdbGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Action", Arrays.asList("action"));
        map.put("Adventure", Arrays.asList("adventure"));
        map.put("Arcade", Arrays.asList("arcade"));
        map.put("Battle Royale", Arrays.asList("battle_royale"));
        map.put("Card & Board Game", Arrays.asList("card", "board_game"));
        map.put("Fighting", Arrays.asList("fighting"));
        map.put("Hack and slash/Beat 'em up", Arrays.asList("hack_and_slash", "beat_em_up"));
        map.put("Hack and Slash", Arrays.asList("hack_and_slash"));
        map.put("Beat 'em up", Arrays.asList("beat_em_up"));
        map.put("Horror", Arrays.asList("horror"));
        map.put("Indie", Arrays.asList("indie"));
        map.put("MOBA", Arrays.asList("moba"));
        map.put("Music", Arrays.asList("music"));
        map.put("Open World", Arrays.asList("open_world"));
        map.put("Pinball", Arrays.asList("arcade"));
        map.put("Platform", Arrays.asList("platform"));
        map.put("Point-and-click", Arrays.asList("point_click", "quest"));
        map.put("Puzzle", Arrays.asList("pazzl"));
        map.put("Quiz/Trivia", Arrays.asList("logic"));
        map.put("Racing", Arrays.asList("racing"));
        map.put("Real Time Strategy (RTS)", Arrays.asList("rts"));
        map.put("Role-playing (RPG)", Arrays.asList("rpg"));
        map.put("Science Fiction", Arrays.asList("sci_fi"));
        map.put("Shooter", Arrays.asList("shooter"));
        map.put("Simulator", Arrays.asList("simulators"));
        map.put("Sport", Arrays.asList("sport"));
        map.put("Stealth", Arrays.asList("stealth"));
        map.put("Strategy", Arrays.asList("strategy"));
        map.put("Survival", Arrays.asList("survival"));
        map.put("Tactical", Arrays.asList("tactics"));
        map.put("Tower Defense", Arrays.asList("tower_defence"));
        map.put("Turn-based strategy (TBS)", Arrays.asList("turn_based", "strategy"));
        map.put("Visual Novel", Arrays.asList("vizualnaia_novella"));
        return map;
    }

    private Map<String, List<String>> buildTheGameDBGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("Action", Arrays.asList("action"));
        map.put("Adventure", Arrays.asList("adventure"));
        map.put("Arcade", Arrays.asList("arcade"));
        map.put("Board Game", Arrays.asList("board_game", "card"));
        map.put("Card Game", Arrays.asList("card"));
        map.put("Educational", Arrays.asList("other"));
        map.put("Family", Arrays.asList("other"));
        map.put("Fighting", Arrays.asList("fighting"));
        map.put("Hack and Slash", Arrays.asList("hack_and_slash", "beat_em_up"));
        map.put("Indie", Arrays.asList("indie"));
        map.put("MOBA", Arrays.asList("moba"));
        map.put("Misc", Arrays.asList("other"));
        map.put("Music", Arrays.asList("music"));
        map.put("Party", Arrays.asList("other"));
        map.put("Pinball", Arrays.asList("arcade"));
        map.put("Platform", Arrays.asList("platform"));
        map.put("Puzzle", Arrays.asList("pazzl"));
        map.put("Racing", Arrays.asList("racing"));
        map.put("Rhythm", Arrays.asList("music"));
        map.put("Role-Playing", Arrays.asList("rpg"));
        map.put("Shooter", Arrays.asList("shooter"));
        map.put("Simulation", Arrays.asList("simulators"));
        map.put("Sports", Arrays.asList("sport"));
        map.put("Strategy", Arrays.asList("strategy"));
        map.put("Trivia", Arrays.asList("logic"));
        map.put("Visual Novel", Arrays.asList("vizualnaia_novella"));
        return map;
    }

    private Map<String, List<String>> buildPsxDataCenterGenreMappings() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("action", Arrays.asList("action"));
        map.put("adventure", Arrays.asList("adventure"));
        map.put("shooter", Arrays.asList("shooter"));
        map.put("simulation", Arrays.asList("simulators"));
        map.put("flight simulator", Arrays.asList("flight", "simulators"));
        map.put("rpg", Arrays.asList("rpg"));
        map.put("strategy", Arrays.asList("strategy"));
        map.put("sports", Arrays.asList("sport"));
        map.put("fighting", Arrays.asList("fighting"));
        map.put("puzzle", Arrays.asList("pazzl"));
        map.put("racing", Arrays.asList("racing"));
        map.put("platform", Arrays.asList("platform"));
        map.put("horror", Arrays.asList("horror"));
        map.put("arcade", Arrays.asList("arcade"));
        map.put("music", Arrays.asList("music"));
        map.put("party", Arrays.asList("other"));
        map.put("trivia", Arrays.asList("logic"));
        map.put("beat'em up", Arrays.asList("beat_em_up"));
        map.put("beat em up", Arrays.asList("beat_em_up"));
        map.put("visual novel", Arrays.asList("vizualnaia_novella"));
        map.put("adult", Arrays.asList("adult"));
        map.put("tactics", Arrays.asList("tactics"));
        map.put("turn based", Arrays.asList("turn_based"));
        map.put("simulator", Arrays.asList("simulators"));
        map.put("open world", Arrays.asList("open_world"));
        map.put("stealth", Arrays.asList("stealth"));
        map.put("survival", Arrays.asList("survival"));
        map.put("battle royale", Arrays.asList("battle_royale"));
        map.put("mmorpg", Arrays.asList("mmo", "rpg"));
        map.put("moba", Arrays.asList("moba"));
        map.put("rail shooter", Arrays.asList("shooter", "arcade"));
        map.put("light gun", Arrays.asList("shooter", "arcade"));
        map.put("dance", Arrays.asList("music"));
        map.put("rhythm", Arrays.asList("music"));
        map.put("board game", Arrays.asList("board_game"));
        map.put("card game", Arrays.asList("card"));
        map.put("fighting (2.5d)", Arrays.asList("fighting"));
        map.put("fighting (3d)", Arrays.asList("fighting"));
        map.put("sandbox", Arrays.asList("sandbox"));
        map.put("life simulation", Arrays.asList("simulators"));
        map.put("vehicle simulation", Arrays.asList("simulators"));
        map.put("space combat", Arrays.asList("space", "shooter"));
        return map;
    }
}

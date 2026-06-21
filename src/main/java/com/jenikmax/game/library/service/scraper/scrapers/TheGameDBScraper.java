package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TheGameDBScraper implements Scraper {

    private final ScraperConfig config;
    private final ConfigEncryptionService encryptionService;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, String> genreCache = new ConcurrentHashMap<>();

    private static final String FIELDS = "genres,overview,players,publishers,rating,coop,youtube";
    private static final String INCLUDE = "boxart";

    public TheGameDBScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
        this.config = config;
        this.encryptionService = encryptionService;
        this.type = config.getType();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) { return gameDto; }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        String gameName = extractGameName(url, gameDto);
        if (gameName == null) return gameDto;
        return scrapByName(gameDto, gameName);
    }

    private GameDto scrapByName(GameDto gameDto, String gameName) {
        try {
            String encKey = config.getEncryptedApiKey();
            String apiKey = encKey != null ? encryptionService.decrypt(encKey) : "";
            if (apiKey == null || apiKey.isEmpty()) return gameDto;

            JsonNode root = searchGame(apiKey, gameName);
            if (root == null) return gameDto;
            JsonNode game = findFirstGame(root);
            if (game == null) return gameDto;

            if (game.has("game_title")) gameDto.setName(game.get("game_title").asText());
            if (game.has("overview")) gameDto.setDescription(game.get("overview").asText());
            if (game.has("release_date")) {
                String dateStr = game.get("release_date").asText();
                if (dateStr.length() >= 4) gameDto.setReleaseDate(dateStr.substring(0, 4));
            }
            String logo = extractLogo(game, root);
            if (logo != null) gameDto.setLogo(logo);
            if (game.has("genres")) {
                List<String> genres = extractGenres(apiKey, game.get("genres"));
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }
            if (game.has("id")) {
                List<String> screenshots = extractScreenshots(apiKey, game.get("id").asInt());
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractGameName(String url, GameDto gameDto) {
        if (url != null && !url.isEmpty()) {
            String[] parts = url.split("/");
            return parts[parts.length - 1].replace("-", " ").replace("_", " ");
        }
        return gameDto.getName();
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String gameName = extractGameName(scrapInfo, gameDto);
        if (gameName == null) return gameDto;

        try {
            String encKey = config.getEncryptedApiKey();
            String apiKey = encKey != null ? encryptionService.decrypt(encKey) : "";
            if (apiKey == null || apiKey.isEmpty()) return gameDto;

            JsonNode root = searchGame(apiKey, gameName);
            if (root == null) return gameDto;
            JsonNode game = findFirstGame(root);
            if (game == null) return gameDto;

            if (scrapInfo.isTitleAttr() && game.has("game_title")) {
                gameDto.setName(game.get("game_title").asText());
            }

            if (scrapInfo.isDescriptionAttr() && game.has("overview")) {
                gameDto.setDescription(game.get("overview").asText());
            }

            if (scrapInfo.isYearAttrAttr() && game.has("release_date")) {
                String dateStr = game.get("release_date").asText();
                if (dateStr.length() >= 4) {
                    gameDto.setReleaseDate(dateStr.substring(0, 4));
                }
            }

            if (scrapInfo.isPosterAttr()) {
                String logo = extractLogo(game, root);
                if (logo != null) {
                    gameDto.setLogo(logo);
                }
            }

            if (scrapInfo.isGenresAttr() && game.has("genres")) {
                List<String> genres = extractGenres(apiKey, game.get("genres"));
                if (!genres.isEmpty()) {
                    gameDto.setGenres(genres);
                }
            }

            if (scrapInfo.isScreensAttr() && game.has("id")) {
                List<String> screenshots = extractScreenshots(apiKey, game.get("id").asInt());
                if (!screenshots.isEmpty()) {
                    gameDto.setScreenshots(screenshots);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private JsonNode searchGame(String apiKey, String gameName) throws IOException {
        String encodedName = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        String baseUrl = config.getApiUrl();
        if (baseUrl == null) baseUrl = "https://api.thegamesdb.net/v1/Games/ByGameName";
        String url = String.format("%s?apikey=%s&name=%s&fields=%s&include=%s",
                baseUrl, apiKey, encodedName, FIELDS, INCLUDE);

        Request request = new Request.Builder().url(url).get().build();
        Response response = client.newCall(request).execute();
        String responseData = response.body().string();
        return mapper.readTree(responseData);
    }

    private JsonNode findFirstGame(JsonNode root) {
        if (root.has("data") && root.get("data").has("games")
                && root.get("data").get("games").size() > 0) {
            return root.get("data").get("games").get(0);
        }
        return null;
    }

    private String extractLogo(JsonNode game, JsonNode root) {
        if (!game.has("id")) return null;
        int gameId = game.get("id").asInt();
        String gameIdStr = String.valueOf(gameId);

        if (root.has("include") && root.get("include").has("boxart")) {
            JsonNode boxart = root.get("include").get("boxart");
            JsonNode data = boxart.get("data").get(gameIdStr);
            if (data != null && data.isArray() && data.size() > 0) {
                String baseUrl = boxart.get("base_url").get("medium").asText();
                String filename = null;
                for (JsonNode item : data) {
                    String side = item.has("side") ? item.get("side").asText() : "";
                    if ("front".equals(side)) {
                        filename = item.get("filename").asText();
                        break;
                    }
                }
                if (filename == null) {
                    filename = data.get(0).get("filename").asText();
                }
                return imageToBase64(baseUrl + filename);
            }
        }
        return null;
    }

    private List<String> extractGenres(String apiKey, JsonNode genreIds) throws IOException {
        if (!genreIds.isArray() || genreIds.size() == 0) return Collections.emptyList();

        Set<String> result = new LinkedHashSet<>();
        List<Integer> uncached = new ArrayList<>();
        Map<Integer, String> resolved = new HashMap<>();

        for (JsonNode idNode : genreIds) {
            int id = idNode.asInt();
            String name = genreCache.get(id);
            if (name != null) {
                resolved.put(id, name);
            } else {
                uncached.add(id);
            }
        }

        if (!uncached.isEmpty()) {
            resolveGenres(apiKey, uncached, resolved);
        }

        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();

        for (String name : resolved.values()) {
            List<String> mapped = mappings.get(name);
            if (mapped != null) {
                result.addAll(mapped);
            }
        }

        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    private void resolveGenres(String apiKey, List<Integer> ids, Map<Integer, String> resolved) throws IOException {
        StringBuilder idParam = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) idParam.append(",");
            idParam.append(ids.get(i));
        }

        String url = String.format("https://api.thegamesdb.net/v1/Genres/ByGenreID?apikey=%s&id=%s",
                apiKey, idParam);
        Request request = new Request.Builder().url(url).get().build();
        Response response = client.newCall(request).execute();
        String responseData = response.body().string();
        JsonNode root = mapper.readTree(responseData);

        if (root.has("data") && root.get("data").has("genres")) {
            JsonNode genres = root.get("data").get("genres");
            Iterator<String> fieldNames = genres.fieldNames();
            while (fieldNames.hasNext()) {
                String key = fieldNames.next();
                JsonNode genreNode = genres.get(key);
                int id = genreNode.get("id").asInt();
                String name = genreNode.get("name").asText();
                genreCache.put(id, name);
                resolved.put(id, name);
            }
        }
    }

    private List<String> extractScreenshots(String apiKey, int gameId) {
        int max = config.getMaxScreenshots();
        List<String> result = new ArrayList<>();

        try {
            String url = String.format(
                    "https://api.thegamesdb.net/v1/Games/Images?apikey=%s&games_id=%d&filter[type]=screenshot",
                    apiKey, gameId);
            Request request = new Request.Builder().url(url).get().build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JsonNode root = mapper.readTree(responseData);

            if (!root.has("data") || !root.get("data").has("images")) return result;

            String baseUrl = root.get("data").get("base_url").get("original").asText();
            JsonNode images = root.get("data").get("images").get(String.valueOf(gameId));
            if (images == null || !images.isArray()) return result;

            int count = 0;
            for (JsonNode img : images) {
                if (count >= max) break;
                String filename = img.get("filename").asText();
                if (filename == null || filename.isEmpty()) continue;
                String fullUrl = baseUrl + filename;
                String b64 = imageToBase64(fullUrl);
                if (b64 != null) {
                    result.add(b64);
                    count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String imageToBase64(String imageUrl) {
        try {
            OkHttpClient imgClient = new OkHttpClient.Builder()
                    .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .build();
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build();
            try (Response response = imgClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                byte[] bytes = response.body().bytes();
                String mime = response.header("Content-Type", "image/jpeg");
                if (mime == null) mime = "image/jpeg";
                int semi = mime.indexOf(';');
                if (semi > 0) mime = mime.substring(0, semi).trim();
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String extractGameName(ScrapInfo scrapInfo, GameDto gameDto) {
        if (scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty()) {
            String[] parts = scrapInfo.getUrl().split("/");
            return parts[parts.length - 1].replace("-", " ").replace("_", " ");
        }
        return gameDto.getName();
    }
}

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LaunchBoxScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(LaunchBoxScraper.class);
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;
    private static final Pattern GAME_KEY_PATTERN = Pattern.compile("/details/(\\d+)");
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    private static final String SEARCH_API = "https://gamesdb-api.launchbox-app.com/api/search";
    private static final String DETAILS_API = "https://gamesdb-api.launchbox-app.com/api/games/details";
    private static final String LEGACY_IMAGE_URL = "https://images.launchbox-app.com/";
    private static final String CLOUDFLARE_IMAGE_URL = "https://gamesdb-images.launchbox.gg/";

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public LaunchBoxScraper(ScraperConfig config, ConfigEncryptionService encryptionService,
                            OkHttpClient client) {
        this.config = config;
        this.client = client;
        this.type = config.getType();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) {
        if (gameDto.getName() == null || gameDto.getName().isEmpty()) return gameDto;
        try {
            String gameKey = searchGameKey(gameDto.getName());
            if (gameKey == null) return gameDto;
            return scrap(gameDto, "https://gamesdb.launchbox-app.com/games/details/" + gameKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        String gameKey = extractGameKey(url);
        if (gameKey == null) {
            try {
                gameKey = searchGameKey(url);
            } catch (Exception e) {
                return gameDto;
            }
        }
        if (gameKey == null) return gameDto;

        try {
            JsonNode data = fetchGameDetails(gameKey);
            if (data == null) return gameDto;

            if (data.has("name")) gameDto.setName(data.get("name").asText());

            if (data.has("overview")) {
                gameDto.setDescription(data.get("overview").asText());
            }

            if (data.has("releaseDate")) {
                String dateStr = data.get("releaseDate").asText();
                Matcher m = YEAR_PATTERN.matcher(dateStr);
                if (m.find()) gameDto.setReleaseDate(m.group(1));
            }

            setCoverImage(gameDto, data);

            if (data.has("gameGenres")) {
                List<String> genres = extractGenres(data.get("gameGenres"));
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }

            if (data.has("videoUrl") && !data.get("videoUrl").isNull()) {
                gameDto.setTrailerUrl(data.get("videoUrl").asText());
            }

            if (data.has("gameImages")) {
                List<String> screenshots = extractAllImages(data.get("gameImages"));
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String gameKey = extractGameKey(scrapInfo.getUrl());
        if (gameKey == null) {
            String gameName = extractGameName(scrapInfo);
            if (gameName == null || gameName.isEmpty()) {
                gameName = gameDto.getName();
            }
            if (gameName != null) {
                try {
                    gameKey = searchGameKey(gameName);
                } catch (Exception e) {
                    return gameDto;
                }
            }
        }
        if (gameKey == null) return gameDto;

        try {
            JsonNode data = fetchGameDetails(gameKey);
            if (data == null) return gameDto;

            if (scrapInfo.isTitleAttr() && data.has("name")) {
                gameDto.setName(data.get("name").asText());
            }

            if (scrapInfo.isDescriptionAttr() && data.has("overview")) {
                gameDto.setDescription(data.get("overview").asText());
            }

            if (scrapInfo.isYearAttrAttr() && data.has("releaseDate")) {
                String dateStr = data.get("releaseDate").asText();
                Matcher m = YEAR_PATTERN.matcher(dateStr);
                if (m.find()) gameDto.setReleaseDate(m.group(1));
            }

            if (scrapInfo.isPosterAttr()) {
                setCoverImage(gameDto, data);
            }

            if (scrapInfo.isGenresAttr() && data.has("gameGenres")) {
                List<String> genres = extractGenres(data.get("gameGenres"));
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }

            if (scrapInfo.isScreensAttr() && data.has("gameImages")) {
                List<String> screenshots = extractAllImages(data.get("gameImages"));
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }

            if (data.has("videoUrl") && !data.get("videoUrl").isNull()
                    && gameDto.getTrailerUrl() == null) {
                gameDto.setTrailerUrl(data.get("videoUrl").asText());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String searchGameKey(String query) throws Exception {
        String searchUrl = SEARCH_API + "/" + java.net.URLEncoder.encode(query, StandardCharsets.UTF_8)
                .replace("+", "%20");
        Request request = new Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            String responseData = response.body().string();
            JsonNode root = mapper.readTree(responseData);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray() || data.size() == 0) return null;
            return String.valueOf(data.get(0).get("gameKey").asLong());
        }
    }

    private JsonNode fetchGameDetails(String gameKey) throws Exception {
        String detailsUrl = DETAILS_API + "/" + gameKey;
        Request request = new Request.Builder()
                .url(detailsUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return null;
            String responseData = response.body().string();
            return mapper.readTree(responseData);
        }
    }

    private void setCoverImage(GameDto gameDto, JsonNode data) {
        if (!data.has("gameImages")) return;
        JsonNode boxFrontNA = null;
        JsonNode boxFrontFirst = null;
        for (JsonNode image : data.get("gameImages")) {
            int typeKey = image.get("imageTypeKey").asInt();
            if (typeKey != 11) continue;
            String region = image.has("regionName") && !image.get("regionName").isNull()
                    ? image.get("regionName").asText() : "";
            if ("North America".equalsIgnoreCase(region)) {
                boxFrontNA = image;
                break;
            }
            if (boxFrontFirst == null) {
                boxFrontFirst = image;
            }
        }
        JsonNode selected = boxFrontNA != null ? boxFrontNA : boxFrontFirst;
        if (selected == null) return;
        String fileName = selected.get("fullGameImageFileName").asText();
        String imageUrl = resolveImageUrl(fileName);
        String b64 = imageToBase64(imageUrl);
        if (b64 != null) gameDto.setLogo(b64);
    }

    private String resolveImageUrl(String fileName) {
        if (fileName.startsWith("r2_")) {
            return CLOUDFLARE_IMAGE_URL + fileName;
        }
        return LEGACY_IMAGE_URL + fileName;
    }

    private List<String> extractGenres(JsonNode genres) {
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();
        Set<String> result = new LinkedHashSet<>();
        for (JsonNode genre : genres) {
            JsonNode nameNode = genre.get("name");
            if (nameNode == null) continue;
            String name = nameNode.asText();
            if (name.isEmpty()) continue;
            List<String> mapped = mappings.get(name);
            if (mapped != null) {
                result.addAll(mapped);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    private List<String> extractAllImages(JsonNode images) {
        int max = config.getMaxScreenshots() > 0
                ? config.getMaxScreenshots() : 40;
        int count = 0;
        List<String> result = new ArrayList<>();
        for (JsonNode image : images) {
            if (count >= max) break;
            String fileName = image.has("fullGameImageFileName")
                    ? image.get("fullGameImageFileName").asText()
                    : null;
            if (fileName == null) {
                fileName = image.has("imageFileName")
                        ? image.get("imageFileName").asText()
                        : null;
            }
            if (fileName == null) continue;
            String imageUrl = resolveImageUrl(fileName);
            String b64 = imageToBase64(imageUrl);
            if (b64 != null) {
                result.add(b64);
                count++;
            }
        }
        return result;
    }

    private String extractGameKey(String url) {
        if (url == null) return null;
        Matcher m = GAME_KEY_PATTERN.matcher(url);
        if (m.find()) return m.group(1);
        try {
            Long.parseLong(url.trim());
            return url.trim();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractGameName(ScrapInfo scrapInfo) {
        if (scrapInfo.getUrl() == null || scrapInfo.getUrl().isEmpty()) return null;
        String url = scrapInfo.getUrl();
        if (url.startsWith("http")) {
            String[] parts = url.split("/");
            String lastPart = parts[parts.length - 1];
            if (lastPart.contains("-")) {
                String[] pieces = lastPart.split("-");
                if (pieces.length > 0) {
                    try {
                        Long.parseLong(pieces[0]);
                        StringBuilder sb = new StringBuilder();
                        for (int i = 1; i < pieces.length; i++) {
                            if (sb.length() > 0) sb.append(' ');
                            sb.append(pieces[i]);
                        }
                        String decoded = URLDecoder.decode(sb.toString(), StandardCharsets.UTF_8);
                        return decoded;
                    } catch (NumberFormatException e) {
                    }
                }
            }
            return lastPart.replace("-", " ").replace("_", " ");
        }
        return url;
    }

    private String imageToBase64(String imageUrl) {
        try {
            okhttp3.OkHttpClient imageClient = client;
            int timeoutMs = config.getTimeoutMs();
            if (timeoutMs > 0 && timeoutMs != client.readTimeoutMillis()) {
                imageClient = client.newBuilder()
                        .readTimeout(java.time.Duration.ofMillis(timeoutMs))
                        .connectTimeout(java.time.Duration.ofMillis(timeoutMs))
                        .build();
            }
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build();
            try (Response response = imageClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.warn("LaunchBox image HTTP {} for {}", response.code(), imageUrl);
                    return null;
                }
                long contentLength = response.body().contentLength();
                if (contentLength > MAX_IMAGE_BYTES) {
                    log.debug("LaunchBox image too large ({} bytes): {}", contentLength, imageUrl);
                    return null;
                }
                byte[] bytes = response.body().bytes();
                if (bytes.length > MAX_IMAGE_BYTES) {
                    log.debug("LaunchBox image too large ({} bytes): {}", bytes.length, imageUrl);
                    return null;
                }
                String mime = response.header("Content-Type", "image/jpeg");
                if (mime == null) mime = "image/jpeg";
                int semi = mime.indexOf(';');
                if (semi > 0) mime = mime.substring(0, semi).trim();
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            log.warn("LaunchBox image download failed: {} — {}", imageUrl, e.toString());
            return null;
        }
    }
}

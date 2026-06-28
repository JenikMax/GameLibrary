package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IGDBScraper implements Scraper {

    private final ScraperConfig config;
    private final ConfigEncryptionService encryptionService;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public IGDBScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
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
        return scrap(gameDto);
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String gameName = extractGameName(scrapInfo);
        if (gameName == null || gameName.isEmpty()) {
            gameName = gameDto.getName();
        }
        if (gameName == null) return gameDto;

        try {
            String encKey = config.getEncryptedApiKey();
            String accessToken = encKey != null ? encryptionService.decrypt(encKey) : "";
            if (accessToken == null) accessToken = "";
            String clientId = config.getHeaders() != null ? config.getHeaders().get("Client-ID") : "";

            String query = String.format(
                    "search \"%s\"; fields name,summary,cover.url,genres.name,first_release_date,screenshots.url; limit 1;",
                    gameName.replace("\"", "\\\"")
            );
            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .header("Client-ID", clientId)
                    .header("Authorization", config.getAuthScheme() + " " + accessToken)
                    .post(RequestBody.create(MediaType.parse("text/plain"), query))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                String responseData = response.body().string();
                JsonNode jsonNode = mapper.readTree(responseData);

                if (jsonNode.isArray() && jsonNode.size() > 0) {
                    JsonNode game = jsonNode.get(0);

                    if (scrapInfo.isTitleAttr() && game.has("name")) {
                        gameDto.setName(game.get("name").asText());
                    }

                    if (scrapInfo.isDescriptionAttr() && game.has("summary")) {
                        gameDto.setDescription(game.get("summary").asText());
                    }

                    if (scrapInfo.isPosterAttr() && game.has("cover") && game.get("cover").has("url")) {
                        String coverUrl = game.get("cover").get("url").asText();
                        String fullUrl = "https:" + coverUrl.replace("t_thumb", "t_cover_big");
                        String b64 = imageToBase64(fullUrl);
                        if (b64 != null) {
                            gameDto.setLogo(b64);
                        }
                    }

                    if (scrapInfo.isYearAttrAttr() && game.has("first_release_date")) {
                        long unix = game.get("first_release_date").asLong();
                        String year = Instant.ofEpochSecond(unix)
                                .atZone(ZoneId.of("UTC"))
                                .format(DateTimeFormatter.ofPattern("yyyy"));
                        gameDto.setReleaseDate(year);
                    }

                    if (scrapInfo.isGenresAttr() && game.has("genres")) {
                        List<String> genres = extractGenres(game.get("genres"));
                        if (!genres.isEmpty()) {
                            gameDto.setGenres(genres);
                        }
                    }

                    if (scrapInfo.isScreensAttr() && game.has("screenshots")) {
                        List<String> screenshots = extractScreenshots(game.get("screenshots"));
                        if (!screenshots.isEmpty()) {
                            gameDto.setScreenshots(screenshots);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
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

    private List<String> extractScreenshots(JsonNode screenshots) {
        int max = config.getMaxScreenshots();
        int count = 0;
        List<String> result = new ArrayList<>();
        for (JsonNode ss : screenshots) {
            if (count >= max) break;
            JsonNode urlNode = ss.get("url");
            if (urlNode == null) continue;
            String url = urlNode.asText();
            if (url.isEmpty()) continue;
            String httpsUrl = "https:" + url.replace("t_thumb", "t_screenshot_huge");
            String b64 = imageToBase64(httpsUrl);
            if (b64 != null) {
                result.add(b64);
                count++;
            }
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

    private String extractGameName(ScrapInfo scrapInfo) {
        if (scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty()) {
            String[] parts = scrapInfo.getUrl().split("/");
            return parts[parts.length - 1].replace("-", " ").replace("_", " ");
        }
        return null;
    }
}

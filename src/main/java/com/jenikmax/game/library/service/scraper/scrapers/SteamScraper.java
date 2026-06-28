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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.context.i18n.LocaleContextHolder;

public class SteamScraper implements Scraper {

    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");

    public SteamScraper(ScraperConfig config, ConfigEncryptionService encryptionService,
                        OkHttpClient client) {
        this.config = config;
        this.client = client;
        this.type = config.getType();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) { return gameDto; }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        String appId = extractAppId(url);
        if (appId == null) return gameDto;

        try {
            JsonNode data = fetchAppData(appId);
            if (data == null) return gameDto;

            if (data.has("name")) gameDto.setName(data.get("name").asText());

            if (data.has("short_description")) {
                gameDto.setDescription(data.get("short_description").asText());
            }

            if (data.has("header_image")) {
                String b64 = imageToBase64(data.get("header_image").asText());
                if (b64 != null) gameDto.setLogo(b64);
            }

            if (data.has("release_date") && data.get("release_date").has("date")) {
                String dateStr = data.get("release_date").get("date").asText();
                Matcher m = YEAR_PATTERN.matcher(dateStr);
                if (m.find()) gameDto.setReleaseDate(m.group(1));
            }

            if (data.has("genres")) {
                List<String> genres = extractGenres(data.get("genres"));
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }

            if (data.has("screenshots")) {
                List<String> screenshots = extractScreenshots(data.get("screenshots"));
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String appId = extractAppId(scrapInfo.getUrl());
        if (appId == null) return gameDto;

        try {
            JsonNode data = fetchAppData(appId);
            if (data == null) return gameDto;

            if (scrapInfo.isTitleAttr() && data.has("name")) {
                gameDto.setName(data.get("name").asText());
            }

            if (scrapInfo.isDescriptionAttr() && data.has("short_description")) {
                gameDto.setDescription(data.get("short_description").asText());
            }

            if (scrapInfo.isPosterAttr() && data.has("header_image")) {
                String b64 = imageToBase64(data.get("header_image").asText());
                if (b64 != null) gameDto.setLogo(b64);
            }

            if (scrapInfo.isYearAttrAttr() && data.has("release_date") && data.get("release_date").has("date")) {
                String dateStr = data.get("release_date").get("date").asText();
                Matcher m = YEAR_PATTERN.matcher(dateStr);
                if (m.find()) gameDto.setReleaseDate(m.group(1));
            }

            if (scrapInfo.isGenresAttr() && data.has("genres")) {
                List<String> genres = extractGenres(data.get("genres"));
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }

            if (scrapInfo.isScreensAttr() && data.has("screenshots")) {
                List<String> screenshots = extractScreenshots(data.get("screenshots"));
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractAppId(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("app".equals(parts[i])) return parts[i + 1];
        }
        return null;
    }

    private String resolveSteamLanguage() {
        Locale locale = LocaleContextHolder.getLocale();
        String lang = locale.getLanguage();
        switch (lang) {
            case "ru": return "russian";
            case "uk": return "ukrainian";
            case "be": return "belarusian";
            case "bg": return "bulgarian";
            case "cs": return "czech";
            case "da": return "danish";
            case "nl": return "dutch";
            case "en": return "english";
            case "et": return "estonian";
            case "fi": return "finnish";
            case "fr": return "french";
            case "de": return "german";
            case "el": return "greek";
            case "hu": return "hungarian";
            case "it": return "italian";
            case "ja": return "japanese";
            case "ko": return "koreana";
            case "lv": return "latvian";
            case "lt": return "lithuanian";
            case "no": return "norwegian";
            case "pl": return "polish";
            case "pt": return "portuguese";
            case "ro": return "romanian";
            case "es": return "spanish";
            case "sv": return "swedish";
            case "th": return "thai";
            case "tr": return "turkish";
            case "vi": return "vietnamese";
            case "zh": return "schinese";
            default: return "english";
        }
    }

    private JsonNode fetchAppData(String appId) throws IOException {
        String apiUrl = config.getApiUrl() != null
                ? config.getApiUrl()
                : "https://store.steampowered.com/api/appdetails";
        String requestUrl = apiUrl + "?appids=" + appId + "&l=" + resolveSteamLanguage();
        Request request = new Request.Builder()
                .url(requestUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseData = response.body().string();
            JsonNode root = mapper.readTree(responseData);
            JsonNode appNode = root.get(appId);
            if (appNode == null || !appNode.has("success") || !appNode.get("success").asBoolean()) {
                return null;
            }
            return appNode.get("data");
        }
    }

    private List<String> extractGenres(JsonNode genres) {
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();
        Set<String> result = new LinkedHashSet<>();
        for (JsonNode genre : genres) {
            JsonNode desc = genre.get("description");
            if (desc == null) continue;
            String name = desc.asText();
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
            JsonNode pathFull = ss.get("path_full");
            if (pathFull == null) continue;
            String url = pathFull.asText();
            if (url.isEmpty()) continue;
            String b64 = imageToBase64(url);
            if (b64 != null) {
                result.add(b64);
                count++;
            }
        }
        return result;
    }

    private String imageToBase64(String imageUrl) {
        try {
            Request request = new Request.Builder()
                    .url(imageUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                long contentLength = response.body().contentLength();
                if (contentLength > MAX_IMAGE_BYTES) return null;
                byte[] bytes = response.body().bytes();
                if (bytes.length > MAX_IMAGE_BYTES) return null;
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
}

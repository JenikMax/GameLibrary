package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.JsoupHelper;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class PlaygroundScraper implements Scraper {

    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final JsoupHelper jsoupHelper;
    private final ObjectMapper mapper = new ObjectMapper();
    private String currentReferer = "https://www.playground.ru/";

    public PlaygroundScraper(ScraperConfig config, ConfigEncryptionService encryptionService,
                             OkHttpClient client, JsoupHelper jsoupHelper) {
        this.config = config;
        this.client = client;
        this.jsoupHelper = jsoupHelper;
        this.type = config.getType();
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(scrapInfo.getUrl());
            if (scrapInfo.isTitleAttr() && gameData.get("title") != null) {
                gameDto.setName(gameData.get("title").toString());
            }
            if (scrapInfo.isYearAttrAttr()) {
                String year = (String) gameData.get("releaseDate");
                if (year != null) {
                    gameDto.setReleaseDate(year);
                }
            }
            if (scrapInfo.isPosterAttr() && gameData.get("posterBase64") != null) {
                gameDto.setLogo((String) gameData.get("posterBase64"));
            }
            if (scrapInfo.isDescriptionAttr() && gameData.get("description") != null) {
                gameDto.setDescription(gameData.get("description").toString());
            }
            if (scrapInfo.isGenresAttr() && gameData.get("genres") != null) {
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            if (scrapInfo.isScreensAttr()) {
                List<String> screens = (List<String>) gameData.get("screenshots");
                if (screens != null && !screens.isEmpty()) {
                    gameDto.setScreenshots(screens);
                } else {
                    gameDto.setScreenshots(getScreenshots(scrapInfo.getUrl()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Playground scrape failed for URL: " + scrapInfo.getUrl(), e);
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(url);
            gameDto.setName(gameData.get("title").toString());
            if (gameData.get("posterBase64") != null) {
                gameDto.setLogo((String) gameData.get("posterBase64"));
            }
            gameDto.setDescription(gameData.get("description").toString());
            String year = (String) gameData.get("releaseDate");
            if (year != null) {
                gameDto.setReleaseDate(year);
            }
            if (gameData.get("genres") != null) {
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            List<String> screens = (List<String>) gameData.get("screenshots");
            if (screens != null && !screens.isEmpty()) {
                gameDto.setScreenshots(screens);
            } else {
                gameDto.setScreenshots(getScreenshots(url));
            }
        } catch (Exception e) {
            throw new RuntimeException("Playground scrape failed for URL: " + url, e);
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto) {
        return gameDto;
    }

    public Map<String, Object> scrapeGameInfo(String gameUrl) throws IOException {
        Map<String, Object> gameData = new HashMap<>();
        this.currentReferer = gameUrl;
        Document document = jsoupHelper.fetchDocument(gameUrl, config);

        JsonNode ldJson = parseJsonLd(document);
        if (ldJson != null) {
            gameData.put("title", ldJson.has("name") ? ldJson.get("name").asText() : "");
            gameData.put("description", ldJson.has("description") ? ldJson.get("description").asText() : "");

            if (ldJson.has("image")) {
                try {
                    JsonNode imageNode = ldJson.get("image");
                    String imageUrl = imageNode.isTextual() ? imageNode.asText() : null;
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        gameData.put("posterBase64", imageToBase64(imageUrl, gameUrl));
                    }
                } catch (Exception e) {
                    // poster failed, continue without it
                }
            }
        }

        List<String> genres = extractGenresFromLinks(document);
        if (!genres.isEmpty()) {
            gameData.put("genres", genres);
        } else if (ldJson != null && ldJson.has("genre")) {
            List<String> ldGenres = new ArrayList<>();
            Map<String, List<String>> mappings = config.getGenreMappings();
            if (mappings == null) mappings = Collections.emptyMap();
            for (JsonNode g : ldJson.get("genre")) {
                String key = g.asText().toLowerCase().replace(" ", "_");
                List<String> mapped = mappings.get(key);
                if (mapped != null) {
                    ldGenres.addAll(mapped);
                } else {
                    try {
                        Genre.valueOf(key);
                        ldGenres.add(key);
                    } catch (IllegalArgumentException e) {
                        // unknown genre, skip
                    }
                }
            }
            gameData.put("genres", ldGenres);
        }

        List<String> screenshots = extractScreenshotsFromDocument(document, gameUrl);
        gameData.put("screenshots", screenshots);

        String year = extractReleaseYear(document);
        gameData.put("releaseDate", year != null ? year : "N/A");

        return gameData;
    }

    private List<String> extractGenresFromLinks(Document document) {
        Set<String> result = new LinkedHashSet<>();
        Elements links = document.select("div.genres a[href^=\"/games/\"], div.genres a[href^=\"https://www.playground.ru/games/\"]");
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();

        for (Element link : links) {
            String href = link.attr("href");
            String slug = null;
            if (href.startsWith("/games/")) {
                slug = href.substring(7);
            } else if (href.startsWith("https://www.playground.ru/games/")) {
                slug = href.substring(35);
            }
            if (slug == null || slug.isEmpty() || slug.contains("/")) continue;

            List<String> mapped = mappings.get(slug);
            if (mapped != null) {
                result.addAll(mapped);
            } else {
                result.add(slug.replace("-", "_"));
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> extractScreenshotsFromDocument(Document doc, String referer) {
        Set<String> urls = new LinkedHashSet<>();
        Elements links = doc.select("a[href*=\"/i/screenshot/\"]");
        for (Element link : links) {
            String url = link.attr("href");
            if (!url.isEmpty() && !url.startsWith("data:")) urls.add(normalizeScreenshotUrl(url));
        }
        Elements imgs = doc.select("img[src*=\"/i/screenshot/\"]");
        for (Element img : imgs) {
            String url = img.attr("src");
            if (!url.isEmpty() && !url.startsWith("data:")) urls.add(normalizeScreenshotUrl(url));
        }
        List<String> result = new ArrayList<>();
        int max = config.getMaxScreenshots();
        int count = 0;
        for (String url : urls) {
            if (count >= max) break;
            try {
                result.add(imageToBase64(url, referer));
                count++;
            } catch (Exception e) {
                // skip failed image
            }
        }
        return result;
    }

    private String normalizeScreenshotUrl(String url) {
        url = url.replace("https://i.playground.ru//i/", "https://i.playground.ru/i/");
        url = url.replace("http://i.playground.ru//i/", "https://i.playground.ru/i/");
        if (url.contains(".webp?")) {
            url = url.substring(0, url.indexOf(".webp?")) + url.substring(url.indexOf(".webp?") + 5);
        }
        int qIdx = url.indexOf('?');
        if (qIdx >= 0) url = url.substring(0, qIdx);
        return url;
    }

    private JsonNode parseJsonLd(Document document) {
        try {
            Elements scripts = document.select("script[type=\"application/ld+json\"]");
            for (Element script : scripts) {
                JsonNode node = mapper.readTree(script.html());
                if (node.has("@type") && "VideoGame".equals(node.get("@type").asText())) {
                    return node;
                }
            }
        } catch (Exception e) {
            // JSON-LD parsing failed
        }
        return null;
    }

    private String extractReleaseYear(Document document) {
        try {
            String text = document.text();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "\\b(0[1-9]|[12]\\d|3[01])\\.(0[1-9]|1[0-2])\\.(19\\d{2}|20\\d{2})\\b");
            java.util.regex.Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group(3);
            }
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private List<String> getScreenshots(String gameUrl) throws IOException {
        String screenUrl = gameUrl.substring(0, gameUrl.lastIndexOf('/')) + "/gallery" + gameUrl.substring(gameUrl.lastIndexOf('/'));
        Document document = jsoupHelper.fetchDocument(screenUrl, config);
        return extractScreenshotsFromDocument(document, gameUrl);
    }

    public String imageToBase64(String imageUrl, String referer) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", referer != null ? referer : currentReferer)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .build();
        try (okhttp3.Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
            long contentLength = response.body().contentLength();
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new IOException("Image too large: " + contentLength + " bytes");
            }
            byte[] bytes = response.body().bytes();
            if (bytes.length > MAX_IMAGE_BYTES) {
                throw new IOException("Image too large: " + bytes.length + " bytes");
            }
            String mime = response.header("Content-Type", "image/jpeg");
            int semi = mime.indexOf(';');
            if (semi > 0) mime = mime.substring(0, semi).trim();
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        }
    }

    @Override
    public String getType() {
        return type;
    }
}

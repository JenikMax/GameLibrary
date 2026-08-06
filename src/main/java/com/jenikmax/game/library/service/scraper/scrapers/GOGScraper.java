package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.JsoupHelper;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GOGScraper implements Scraper {

    private static final Logger log = LoggerFactory.getLogger(GOGScraper.class);
    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;
    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");
    private static final Pattern DATE_ISO_PATTERN = Pattern.compile("'(\\d{4}-\\d{2}-\\d{2})T");

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final JsoupHelper jsoupHelper;
    private final ObjectMapper mapper = new ObjectMapper();

    public GOGScraper(ScraperConfig config, ConfigEncryptionService encryptionService,
                      OkHttpClient client, JsoupHelper jsoupHelper) {
        this.config = config;
        this.client = client;
        this.jsoupHelper = jsoupHelper;
        this.type = config.getType();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) {
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Document doc = loadPage(url);
            if (doc == null) return gameDto;

            scrapeAllFields(gameDto, doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String url = scrapInfo.getUrl();
        if (url == null || url.isEmpty()) return gameDto;

        try {
            Document doc = loadPage(url);
            if (doc == null) return gameDto;

            if (scrapInfo.isTitleAttr()) {
                String title = extractTitle(doc);
                if (title != null) gameDto.setName(title);
            }

            if (scrapInfo.isDescriptionAttr()) {
                String desc = extractDescription(doc);
                if (desc != null) gameDto.setDescription(desc);
            }

            if (scrapInfo.isYearAttrAttr()) {
                String year = extractReleaseYear(doc);
                if (year != null) gameDto.setReleaseDate(year);
            }

            if (scrapInfo.isPosterAttr()) {
                String cover = extractCover(doc);
                if (cover != null) gameDto.setLogo(cover);
            }

            if (scrapInfo.isGenresAttr()) {
                List<String> genres = extractGenresAndTags(doc);
                if (!genres.isEmpty()) gameDto.setGenres(genres);
            }

            if (scrapInfo.isScreensAttr()) {
                List<String> screenshots = extractScreenshots(doc);
                if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private void scrapeAllFields(GameDto gameDto, Document doc) {
        String title = extractTitle(doc);
        if (title != null) gameDto.setName(title);

        String desc = extractDescription(doc);
        if (desc != null) gameDto.setDescription(desc);

        String year = extractReleaseYear(doc);
        if (year != null) gameDto.setReleaseDate(year);

        List<String> genres = extractGenresAndTags(doc);
        if (!genres.isEmpty()) gameDto.setGenres(genres);

        String cover = extractCover(doc);
        if (cover != null) gameDto.setLogo(cover);

        List<String> screenshots = extractScreenshots(doc);
        if (!screenshots.isEmpty()) gameDto.setScreenshots(screenshots);
    }

    private Document loadPage(String url) throws Exception {
        if (url.startsWith("http")) {
            return jsoupHelper.fetchDocument(url, config);
        }
        String cleanSlug = url.replace("https://", "").replace("http://", "")
                .replace("www.gog.com", "").replace("/game/", "")
                .replace("/en/", "").replace("/ru/", "").replace("/de/", "")
                .replace("/fr/", "").replace("/pl/", "").replace("/zh/", "")
                .replace("/es/", "");
        return jsoupHelper.fetchDocument("https://www.gog.com/game/" + cleanSlug, config);
    }

    private String extractTitle(Document doc) {
        String title = doc.select("meta[property=\"og:title\"]").attr("content");
        if (title != null && !title.isEmpty()) {
            return title.replaceFirst("^(-?\\d+%\\s+)", "").trim();
        }
        try {
            Elements scripts = doc.select("script:containsData(window.productcardData)");
            for (Element script : scripts) {
                String data = script.data();
                int start = data.indexOf("window.productcardData");
                if (start >= 0) {
                    int titleStart = data.indexOf("\"title\":\"", start);
                    if (titleStart >= 0) {
                        int titleEnd = data.indexOf("\"", titleStart + 9);
                        if (titleEnd >= 0) {
                            return data.substring(titleStart + 9, titleEnd);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        title = doc.title();
        if (title != null && !title.isEmpty()) {
            return title.replaceAll(" на GOG.com$", "").replaceAll(" on GOG.com$", "").trim();
        }
        return null;
    }

    private String extractDescription(Document doc) {
        Elements descDiv = doc.select("[selenium-id=\"ProductCardDescription\"] .description");
        if (!descDiv.isEmpty()) {
            String html = descDiv.first().html();
            if (html != null && !html.isEmpty()) {
                return org.jsoup.Jsoup.parse(html).text();
            }
        }

        try {
            Elements scripts = doc.select("script:containsData(window.productcardData)");
            for (Element script : scripts) {
                String data = script.data();
                int start = data.indexOf("window.productcardData");
                if (start < 0) continue;
                int cardStart = data.indexOf("\"cardProduct\":", start);
                if (cardStart < 0) cardStart = start;
                int descStart = data.indexOf("\"description\":\"", cardStart);
                if (descStart < 0) continue;
                int valueStart = descStart + 15;
                StringBuilder sb = new StringBuilder();
                for (int i = valueStart; i < data.length(); i++) {
                    char c = data.charAt(i);
                    if (c == '\\' && i + 1 < data.length()) {
                        char next = data.charAt(i + 1);
                        if (next == 'n') { sb.append('\n'); i++; }
                        else if (next == '"') { sb.append('"'); i++; }
                        else if (next == '\\') { sb.append('\\'); i++; }
                        else if (next == 't') { sb.append('\t'); i++; }
                        else if (next == 'r') { i++; }
                        else { sb.append(c); }
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                String fullDesc = sb.toString().trim();
                if (!fullDesc.isEmpty()) {
                    return org.jsoup.Jsoup.parse(fullDesc).text();
                }
            }
        } catch (Exception ignored) {
        }

        String desc = doc.select("meta[property=\"og:description\"]").attr("content");
        if (desc != null && !desc.isEmpty()) return desc.trim();
        desc = doc.select("meta[name=\"description\"]").attr("content");
        if (desc != null && !desc.isEmpty()) return desc.trim();
        return null;
    }

    private String extractReleaseYear(Document doc) {
        try {
            Elements ldJson = doc.select("script[type=\"application/ld+json\"]");
            for (Element script : ldJson) {
                JsonNode node = mapper.readTree(script.data());
                if (node.has("releaseDate")) {
                    String dateStr = node.get("releaseDate").asText();
                    Matcher m = YEAR_PATTERN.matcher(dateStr);
                    if (m.find()) return m.group(1);
                }
            }
        } catch (Exception ignored) {
        }

        String html = doc.html();
        Matcher dm = DATE_ISO_PATTERN.matcher(html);
        if (dm.find()) {
            Matcher ym = YEAR_PATTERN.matcher(dm.group(1));
            if (ym.find()) return ym.group(1);
        }

        String pageText = doc.text();
        Matcher m = java.util.regex.Pattern
                .compile("(?:Дата выхода|Release date)[:\\s]+(\\d{4})")
                .matcher(pageText);
        if (m.find()) return m.group(1);

        return null;
    }

    private List<String> extractGenresAndTags(Document doc) {
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();
        Set<String> result = new LinkedHashSet<>();

        Elements genreLinks = doc.select("ul.genres a[data-event-genre-slug]");
        for (Element link : genreLinks) {
            String slug = link.attr("data-event-genre-slug").toLowerCase();
            mapSlugToGenres(slug, mappings, result);
        }
        if (genreLinks.isEmpty()) {
            genreLinks = doc.select("a[data-event-genre-slug]");
            for (Element link : genreLinks) {
                String slug = link.attr("data-event-genre-slug").toLowerCase();
                mapSlugToGenres(slug, mappings, result);
            }
        }

        Elements tagSpans = doc.select(".details__link--tag .details__link-text");
        for (Element span : tagSpans) {
            String tagText = span.text().trim().toLowerCase();
            String tagSlug = "";
            Element parentLink = span.parent();
            if (parentLink != null && parentLink.hasAttr("href")) {
                String href = parentLink.attr("href");
                if (href.contains("/tags/")) {
                    tagSlug = href.substring(href.lastIndexOf('/') + 1).toLowerCase();
                }
            }
            mapSlugToGenres(tagSlug.isEmpty() ? tagText : tagSlug, mappings, result);
            if (!tagSlug.isEmpty()) {
                mapSlugToGenres(tagText, mappings, result);
            }
        }

        if (result.isEmpty()) {
            String pageText = doc.text();
            Matcher genreMatcher = java.util.regex.Pattern
                    .compile("(?:Жанр|Genre)[:\\s]+(.+?)(?:Теги|Tags|$)")
                    .matcher(pageText);
            if (genreMatcher.find()) {
                String genreSection = genreMatcher.group(1).trim();
                for (String part : genreSection.split("\\s*-\\s*")) {
                    String g = part.trim().toLowerCase();
                    for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
                        if (entry.getKey().equalsIgnoreCase(g)
                                || g.contains(entry.getKey().toLowerCase())
                                || entry.getKey().toLowerCase().contains(g)) {
                            result.addAll(entry.getValue());
                        }
                    }
                }
            }
        }

        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    private void mapSlugToGenres(String slug, Map<String, List<String>> mappings, Set<String> result) {
        if (slug == null || slug.isEmpty()) return;
        List<String> mapped = mappings.get(slug);
        if (mapped != null) {
            result.addAll(mapped);
            return;
        }
        for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(slug)
                    || entry.getKey().toLowerCase().contains(slug)
                    || slug.contains(entry.getKey().toLowerCase())) {
                result.addAll(entry.getValue());
            }
        }
    }

    private String extractCover(Document doc) {
        String coverUrl = doc.select("meta[property=\"og:image\"]").attr("content");
        if (coverUrl == null || coverUrl.isEmpty()) return null;
        if (coverUrl.startsWith("/")) coverUrl = "https:" + coverUrl;
        String b64 = imageToBase64(coverUrl);
        return b64;
    }

    private List<String> extractScreenshots(Document doc) {
        int max = config.getMaxScreenshots() > 0 ? config.getMaxScreenshots() : 20;
        Elements imgs = doc.select("img[src]");
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (Element img : imgs) {
            if (result.size() >= max) break;
            String src = img.attr("src");
            if (src.isEmpty() || !seen.add(src)) continue;
            if (src.contains("data:") || src.contains("svg") || src.contains("favicon")
                    || src.contains("apple-touch-icon")) continue;
            if (!src.contains("images.gog-statics.com") && !src.contains("gog-statics")) continue;
            if (src.startsWith("/")) {
                src = "https:" + src;
            } else if (!src.startsWith("http")) {
                continue;
            }
            String b64 = imageToBase64(src);
            if (b64 != null) {
                result.add(b64);
            }
        }
        return result;
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
                    log.warn("GOG image HTTP {} for {}", response.code(), imageUrl);
                    return null;
                }
                long contentLength = response.body().contentLength();
                if (contentLength > MAX_IMAGE_BYTES) {
                    log.debug("GOG image too large ({} bytes): {}", contentLength, imageUrl);
                    return null;
                }
                byte[] bytes = response.body().bytes();
                if (bytes.length > MAX_IMAGE_BYTES) {
                    log.debug("GOG image too large ({} bytes): {}", bytes.length, imageUrl);
                    return null;
                }
                String mime = response.header("Content-Type", "image/jpeg");
                if (mime == null) mime = "image/jpeg";
                int semi = mime.indexOf(';');
                if (semi > 0) mime = mime.substring(0, semi).trim();
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            }
        } catch (Exception e) {
            log.warn("GOG image download failed: {} — {}", imageUrl, e.toString());
            return null;
        }
    }
}

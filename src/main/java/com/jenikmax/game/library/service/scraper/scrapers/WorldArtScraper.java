package com.jenikmax.game.library.service.scraper.scrapers;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldArtScraper implements Scraper {

    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;

    public WorldArtScraper(ScraperConfig config, OkHttpClient client) {
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
        try {
            Map<String, Object> gameData = scrapeGameInfo(extractIdFromUrl(url), gameDto);
            if (gameData.get("title") != null) gameDto.setName((String) gameData.get("title"));
            if (gameData.get("posterBase64") != null) gameDto.setLogo((String) gameData.get("posterBase64"));
            if (gameData.get("description") != null) gameDto.setDescription((String) gameData.get("description"));
            if (gameData.get("releaseDate") != null) gameDto.setReleaseDate((String) gameData.get("releaseDate"));
            if (gameData.get("genres") != null) gameDto.setGenres((List<String>) gameData.get("genres"));
            if (gameData.get("screenshots") != null) gameDto.setScreenshots((List<String>) gameData.get("screenshots"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            String gameId = resolveGameId(scrapInfo, gameDto);
            if (gameId == null) return gameDto;

            Map<String, Object> gameData = scrapeGameInfo(gameId, gameDto);

            if (scrapInfo.isTitleAttr() && gameData.get("title") != null) {
                gameDto.setName((String) gameData.get("title"));
            }
            if (scrapInfo.isPosterAttr() && gameData.get("posterBase64") != null) {
                gameDto.setLogo((String) gameData.get("posterBase64"));
            }
            if (scrapInfo.isDescriptionAttr() && gameData.get("description") != null) {
                gameDto.setDescription((String) gameData.get("description"));
            }
            if (scrapInfo.isYearAttrAttr() && gameData.get("releaseDate") != null) {
                gameDto.setReleaseDate((String) gameData.get("releaseDate"));
            }
            if (scrapInfo.isGenresAttr() && gameData.get("genres") != null) {
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            if (scrapInfo.isScreensAttr() && gameData.get("screenshots") != null) {
                gameDto.setScreenshots((List<String>) gameData.get("screenshots"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String resolveGameId(ScrapInfo scrapInfo, GameDto gameDto) throws IOException {
        String url = scrapInfo.getUrl();
        if (url != null && !url.isEmpty()) {
            String id = extractIdFromUrl(url);
            if (id != null) return id;
            return searchByName(gameDto.getName());
        }
        return searchByName(gameDto.getName());
    }

    private String extractIdFromUrl(String url) {
        if (url == null) return null;
        Matcher m = Pattern.compile("[?&]id=(\\d+)").matcher(url);
        if (m.find()) return m.group(1);
        return null;
    }

    private String searchByName(String query) throws IOException {
        if (query == null || query.isEmpty()) return null;
        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "http://www.world-art.ru";
        String encoded = URLEncoder.encode(query, "windows-1251");
        Document doc = fetchDocument(baseUrl + "/games/list.php?searchtext=" + encoded + "&dosearch=");
        if (doc == null) return null;

        Elements results = doc.select(".foundresult");
        for (Element result : results) {
            Element link = result.selectFirst("a[href*=\"games.php?id=\"]");
            if (link != null) {
                Matcher m = Pattern.compile("[?&]id=(\\d+)").matcher(link.attr("href"));
                if (m.find()) return m.group(1);
            }
        }
        return null;
    }

    private Map<String, Object> scrapeGameInfo(String gameId, GameDto gameDto) throws IOException {
        Map<String, Object> data = new HashMap<>();
        if (gameId == null) return data;

        String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "http://www.world-art.ru";
        Document doc = fetchDocument(baseUrl + "/games/games.php?id=" + gameId);
        if (doc == null) return data;

        data.put("title", extractTitle(doc));
        data.put("posterBase64", extractPoster(doc, gameId, baseUrl));
        data.put("description", extractDescription(doc));
        data.put("releaseDate", extractReleaseDate(doc));
        data.put("genres", extractGenres(doc));
        data.put("screenshots", extractScreenshots(gameId, baseUrl));

        return data;
    }

    private String extractTitle(Document doc) {
        String title = doc.title();
        if (title != null && !title.isEmpty()) {
            int idx = title.indexOf(" - игра");
            if (idx > 0) return title.substring(0, idx).trim();
        }
        Element font = doc.selectFirst("font[size=\"5\"]");
        return font != null ? font.text().trim() : null;
    }

    private String extractPoster(Document doc, String gameId, String baseUrl) {
        try {
            Element img = doc.selectFirst("div.comment_block img[src]");
            if (img == null) return null;
            String src = img.attr("src");
            String fullUrl;
            if (src.startsWith("http")) {
                fullUrl = src;
            } else if (src.startsWith("/")) {
                fullUrl = baseUrl + src;
            } else {
                fullUrl = baseUrl + "/games/" + src;
            }
            return imageToBase64(fullUrl);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractDescription(Document doc) {
        Element p = doc.selectFirst("p.review[align=justify]");
        return p != null ? p.text().trim() : null;
    }

    private String extractReleaseDate(Document doc) {
        Element td = findInfoValue(doc, "Релиз в мире");
        if (td == null) td = findInfoValue(doc, "Релиз в России");
        if (td == null) return null;
        String text = td.text().trim();
        Matcher m = Pattern.compile("\\d{4}\\.\\d{2}\\.\\d{2}").matcher(text);
        if (m.find()) return m.group();
        m = Pattern.compile("(\\d{4})").matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    private List<String> extractGenres(Document doc) {
        Set<String> result = new LinkedHashSet<>();
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();

        Element td = findInfoValue(doc, "Жанр");
        if (td != null) {
            String[] parts = td.text().split("\\s*,\\s*");
            for (String part : parts) {
                String trimmed = part.trim().toLowerCase();
                if (trimmed.isEmpty()) continue;
                List<String> mapped = mappings.get(trimmed);
                if (mapped != null) {
                    result.addAll(mapped);
                }
            }
        }

        Elements tags = doc.select("div.newtag a.newtag1");
        for (Element tag : tags) {
            String tagText = tag.text().trim().toLowerCase();
            if (tagText.isEmpty()) continue;
            List<String> mapped = mappings.get(tagText);
            if (mapped != null) {
                result.addAll(mapped);
            }
        }

        return result.isEmpty() ? null : new ArrayList<>(result);
    }

    private List<String> extractScreenshots(String gameId, String baseUrl) {
        try {
            Document doc = fetchDocument(baseUrl + "/games/games_img.php?id=" + gameId);
            if (doc == null) return Collections.emptyList();

            int bucket = ((Integer.parseInt(gameId) + 9999) / 10000) * 10000;
            List<String> result = new ArrayList<>();
            int max = config.getMaxScreenshots();
            int count = 0;

            Elements images = doc.select("div[style*=\"display: flex\"] a img[src*=\"optimize_b\"]");
            for (Element img : images) {
                if (count >= max) break;
                String src = img.attr("src");
                Matcher m = Pattern.compile(gameId + "-(\\d+)-optimize_b").matcher(src);
                if (m.find()) {
                    String num = m.group(1);
                    String fullUrl = baseUrl + "/games/img/" + bucket + "/" + gameId + "/" + num + ".jpg";
                    try {
                        String b64 = imageToBase64(fullUrl);
                        result.add(b64);
                        count++;
                    } catch (Exception e) {
                        // skip failed screenshot
                    }
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Element findInfoValue(Document doc, String label) {
        Elements tables = doc.select("table:has(td.review > b)");
        for (Element table : tables) {
            Element b = table.selectFirst("td.review > b");
            if (b != null && b.text().trim().equals(label)) {
                Elements tds = table.select("td.review");
                if (tds.size() >= 3) {
                    return tds.get(2);
                }
            }
        }
        return null;
    }

    private Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(config.getTimeoutMs())
                .get();
    }

    private String imageToBase64(String imageUrl) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
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
}

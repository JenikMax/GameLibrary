package com.jenikmax.game.library.service.scraper.scrapers;

import com.jenikmax.game.library.model.dto.GameDto;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PsxDataCenterScraper implements Scraper {

    private static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;

    private static final List<SearchSource> SEARCH_SOURCES = Arrays.asList(
        new SearchSource("PS1", "ntsc-u", "https://psxdatacenter.com/ulist.html"),
        new SearchSource("PS1", "pal",    "https://psxdatacenter.com/plist.html"),
        new SearchSource("PS1", "ntsc-j", "https://psxdatacenter.com/jlist.html"),
        new SearchSource("PS2", "ntsc-u", "https://psxdatacenter.com/psx2/ulist2.html"),
        new SearchSource("PS2", "pal",    "https://psxdatacenter.com/psx2/plist2.html"),
        new SearchSource("PS2", "ntsc-j", "https://psxdatacenter.com/psx2/jlist2.html")
    );

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final JsoupHelper jsoupHelper;

    public PsxDataCenterScraper(ScraperConfig config, OkHttpClient client, JsoupHelper jsoupHelper) {
        this.config = config;
        this.client = client;
        this.jsoupHelper = jsoupHelper;
        this.type = config.getType();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) {
        if (gameDto.getName() == null || gameDto.getName().isEmpty()) return gameDto;
        try {
            String url = searchByName(gameDto.getName());
            if (url != null) return scrap(gameDto, url);
        } catch (Exception e) {
            // search failed
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Map<String, Object> data = scrapeGameCard(url);
            if (data.get("title") != null) gameDto.setName((String) data.get("title"));
            if (data.get("posterBase64") != null) gameDto.setLogo((String) data.get("posterBase64"));
            if (data.get("description") != null) gameDto.setDescription((String) data.get("description"));
            if (data.get("releaseDate") != null) gameDto.setReleaseDate((String) data.get("releaseDate"));
            if (data.get("genres") != null) gameDto.setGenres((List<String>) data.get("genres"));
            if (data.get("screenshots") != null) gameDto.setScreenshots((List<String>) data.get("screenshots"));
            if (data.get("instruction") != null) gameDto.setInstruction((String) data.get("instruction"));
        } catch (Exception e) {
            throw new RuntimeException("PsxDataCenter scrape failed: " + e.getMessage(), e);
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            String url = resolveUrl(scrapInfo, gameDto);
            if (url == null) return gameDto;

            Map<String, Object> data = scrapeGameCard(url);

            if (scrapInfo.isTitleAttr() && data.get("title") != null)
                gameDto.setName((String) data.get("title"));
            if (scrapInfo.isPosterAttr() && data.get("posterBase64") != null)
                gameDto.setLogo((String) data.get("posterBase64"));
            if (scrapInfo.isDescriptionAttr() && data.get("description") != null)
                gameDto.setDescription((String) data.get("description"));
            if (scrapInfo.isYearAttrAttr() && data.get("releaseDate") != null)
                gameDto.setReleaseDate((String) data.get("releaseDate"));
            if (scrapInfo.isGenresAttr() && data.get("genres") != null)
                gameDto.setGenres((List<String>) data.get("genres"));
            if (scrapInfo.isScreensAttr() && data.get("screenshots") != null)
                gameDto.setScreenshots((List<String>) data.get("screenshots"));
            if (scrapInfo.isInstructionAttr() && data.get("instruction") != null)
                gameDto.setInstruction((String) data.get("instruction"));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("PsxDataCenter scrape failed", e);
        }
        return gameDto;
    }

    private String resolveUrl(ScrapInfo scrapInfo, GameDto gameDto) throws IOException {
        String url = scrapInfo.getUrl();
        if (url != null && !url.isEmpty()) {
            if (url.startsWith("http://") || url.startsWith("https://")) return url;
            return searchByName(url);
        }
        String name = gameDto.getName();
        if (name != null && !name.isEmpty()) return searchByName(name);
        return null;
    }

    private String searchByName(String name) throws IOException {
        if (name == null || name.isEmpty()) return null;
        String query = name.toLowerCase(Locale.ROOT).trim();

        for (SearchSource ss : SEARCH_SOURCES) {
            Document doc = jsoupHelper.fetchDocument(ss.listUrl, config);
            String url = findInList(doc, query);
            if (url != null) return url;
        }
        return null;
    }

    private String findInList(Document doc, String query) {
        Elements infoLinks = doc.select("a[href]:matchesOwn((?i)^INFO$)");
        for (Element link : infoLinks) {
            Element titleCell = findTitleCell(link);
            if (titleCell == null) continue;
            String title = titleCell.text().replace((char)0xA0, ' ').trim();
            String normalized = title.replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
            String searchNormalized = query.replaceAll("[^a-zA-Z0-9]", "");
            if (normalized.contains(searchNormalized)) {
                return link.absUrl("href");
            }
        }
        return null;
    }

    private Element findTitleCell(Element infoLink) {
        Element row = infoLink.closest("tr");
        if (row == null) return null;
        Elements cells = row.children();
        if (cells.size() < 3) return null;
        return cells.get(2);
    }

    private Map<String, Object> scrapeGameCard(String url) throws IOException {
        Map<String, Object> data = new HashMap<>();
        Document doc = jsoupHelper.fetchDocument(url, config);

        data.put("title", extractTitle(doc));
        data.put("description", extractDescription(doc));
        data.put("releaseDate", extractReleaseDate(doc));
        data.put("posterBase64", extractPoster(doc, url));
        data.put("genres", extractGenres(doc));
        data.put("screenshots", extractScreenshots(doc, url));
        data.put("instruction", extractInstruction(doc));

        return data;
    }

    // ----- Field extractors -----

    private String extractTitle(Document doc) {
        // Version B (no <b>): <td>Common Title</td><td>ACE COMBAT 3</td>
        String val = findValueAfterLabel(doc, "Common Title");
        // Version B fallback: <td>Official Title</td>
        if (val == null) val = findValueAfterLabel(doc, "Official Title");
        // Version A (with <b>): <td><b>Game Name:</b></td><td>...</td>
        if (val == null) val = findValueAfterBoldLabel(doc, "Game Name:");
        // Fallback: <title>
        if (val == null) {
            String fallback = doc.title().replace("PSX DataCenter - ", "").trim();
            if (!fallback.isEmpty()) val = fallback;
        }
        return val != null ? cleanText(val) : null;
    }

    private String extractDescription(Document doc) {
        // Version B (inline styles, no label): table#table16 td[style*=background-color]
        Element td = doc.selectFirst("table#table16 td[style*=\"background-color: #333333\"]");
        if (td != null) {
            String desc = td.text().trim();
            if (desc.length() > 50) return cleanText(desc);
        }
        // Version A (with <b>Game description:</b>)
        Element b = doc.selectFirst("b:matchesOwn((?i)Game description)");
        if (b != null) {
            Element container = b.closest("td");
            if (container != null) {
                String fullText = container.text().trim();
                String desc = fullText.replaceFirst("(?i)Game description[^:]*:\\s*", "").trim();
                desc = cleanText(desc);
                if (desc.length() > 5) return desc;
                // Try next row (PS2)
                Element row = container.parent();
                if (row != null) {
                    Element next = row.nextElementSibling();
                    if (next != null) {
                        Element nextTd = next.selectFirst("td");
                        if (nextTd != null) {
                            desc = nextTd.text().trim();
                            desc = cleanText(desc);
                            if (desc.length() > 5) return desc;
                        }
                    }
                }
            }
        }
        // Last resort: longest text td on the page
        String longest = findLongestTdText(doc);
        if (longest != null) return longest;
        return null;
    }

    private String extractReleaseDate(Document doc) {
        String val = findValueAfterLabel(doc, "Date Released");
        if (val == null) val = findValueAfterBoldLabel(doc, "Release Date:");
        if (val == null) return null;
        val = cleanText(val);
        Matcher m = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b").matcher(val);
        return m.find() ? m.group(1) : val;
    }

    private String extractPoster(Document doc, String referer) {
        try {
            Element img = doc.selectFirst("img[src*=\"images/covers/\"], img[src*=\"images2/covers/\"]");
            if (img == null) return null;
            String src = img.absUrl("src");
            if (src == null || src.isEmpty()) return null;
            return imageToBase64(src, referer);
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> extractGenres(Document doc) {
        Map<String, List<String>> mappings = config.getGenreMappings();
        if (mappings == null) mappings = Collections.emptyMap();

        String raw = null;
        // Version B: <td>Genre / Style</td><td>Shooter / Action / Simulation</td>
        raw = findValueAfterLabel(doc, "Genre / Style");
        // Version A: <td><b>Genre:</b></td><td>Shooter / Action / Simulation</td>
        if (raw == null) raw = findValueAfterBoldLabel(doc, "Genre:");
        // Parse raw genres
        if (raw != null) {
            raw = cleanText(raw);
            Set<String> result = new LinkedHashSet<>();
            String[] parts = raw.split("\\s*/\\s*");
            for (String part : parts) {
                String key = part.trim().toLowerCase(Locale.ROOT);
                key = key.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
                if (key.isEmpty()) continue;
                List<String> mapped = mappings.get(key);
                if (mapped != null) {
                    result.addAll(mapped);
                } else {
                    String cleaned = key.replaceAll("[^a-z0-9_]", "");
                    if (!cleaned.isEmpty()) result.add(cleaned);
                }
            }
            if (!result.isEmpty()) return new ArrayList<>(result);
        }

        // Fallback: Genre Rating links
        Set<String> result = new LinkedHashSet<>();
        for (Element row : doc.select("tr")) {
            Elements tds = row.children();
            boolean hasRating = false;
            for (Element td : tds) {
                if (td.ownText().trim().matches("(?i).*Genre Rating:.*")) {
                    hasRating = true;
                    break;
                }
            }
            if (!hasRating) continue;
            Elements links = row.select("a[href*=\"rating.html\"]");
            for (Element link : links) {
                String genreText = link.text().trim().toLowerCase(Locale.ROOT);
                if (genreText.isEmpty()) continue;
                List<String> mapped = mappings.get(genreText);
                if (mapped != null) {
                    result.addAll(mapped);
                }
            }
        }
        if (!result.isEmpty()) return new ArrayList<>(result);
        return null;
    }

    private List<String> extractScreenshots(Document doc, String referer) {
        Elements imgElements = doc.select("img[src*=\"images/screens/\"], img[src*=\"images2/screens/\"]");
        Set<String> urls = new LinkedHashSet<>();
        for (Element img : imgElements) {
            String src = img.absUrl("src");
            if (src != null && !src.isEmpty()) urls.add(src);
        }
        List<String> result = new ArrayList<>();
        int max = config.getMaxScreenshots();
        int count = 0;
        for (String url : urls) {
            if (count >= max) break;
            String cleanUrl = url.indexOf('?') >= 0 ? url.substring(0, url.indexOf('?')) : url;
            try {
                result.add(imageToBase64(cleanUrl, referer));
                count++;
            } catch (Exception e) {
                // skip failed screenshot
            }
        }
        return result.isEmpty() ? null : result;
    }

    private String extractInstruction(Document doc) {
        // Version A/B: table with bluecell/darkcell containing Emulator
        Element emuCell = doc.selectFirst("td.bluecell:matchesOwn((?i)emulator)");
        if (emuCell == null) {
            emuCell = doc.selectFirst("td:matchesOwn((?i)emulator)");
        }

        Element table = emuCell != null ? emuCell.closest("table") : null;
        if (table == null) {
            // Try direct table#table25
            table = doc.selectFirst("table#table25");
        }
        if (table == null) return null;

        StringBuilder sb = new StringBuilder();
        Elements rows = table.select("tr");
        for (Element row : rows) {
            Elements cells = row.select("td");
            if (cells.size() >= 2) {
                String label = cells.get(0).text().replace((char)0xA0, ' ').trim();
                String value = cells.get(1).text().replace((char)0xA0, ' ').trim();
                if (label.isEmpty() || value.isEmpty() ||
                    label.matches("(?i).*note.*")) continue;
                sb.append(cleanText(label)).append(": ").append(cleanText(value)).append("\n");
            }
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? null : result;
    }

    // ----- Helpers -----

    /** Version B: <td>Label</td><td>Value</td> (label directly in td, no <b>) */
    private String findValueAfterLabel(Document doc, String label) {
        Elements tds = doc.select("td");
        for (Element td : tds) {
            if (label.equalsIgnoreCase(td.ownText().trim())) {
                Element row = td.parent();
                if (row == null) continue;
                Elements cells = row.children();
                int idx = cells.indexOf(td);
                if (idx >= 0 && idx + 1 < cells.size()) {
                    return cells.get(idx + 1).text().trim();
                }
            }
        }
        return null;
    }

    /** Version A: <td><b>Label</b></td><td>Value</td> */
    private String findValueAfterBoldLabel(Document doc, String label) {
        Elements bs = doc.select("b");
        for (Element b : bs) {
            if (label.equalsIgnoreCase(b.ownText().trim())) {
                Element row = b.closest("tr");
                Element td = b.closest("td");
                if (row == null || td == null) continue;
                Elements cells = row.children();
                int idx = cells.indexOf(td);
                if (idx >= 0 && idx + 1 < cells.size()) {
                    return cells.get(idx + 1).text().trim();
                }
            }
        }
        return null;
    }

    private String findLongestTdText(Document doc) {
        String best = null;
        int bestLen = 0;
        for (Element td : doc.select("td")) {
            String text = td.text().trim();
            if (text.length() > bestLen && text.length() > 200) {
                // Exclude known sections
                if (text.startsWith("Number Of Players") || text.startsWith("Emulator") ||
                    text.startsWith("Gameshark") || text.startsWith("ANALOG CONTROLLER") ||
                    text.startsWith("CLICK THE IMAGE")) continue;
                best = text;
                bestLen = text.length();
            }
        }
        return best != null ? cleanText(best) : null;
    }

    private String cleanText(String text) {
        if (text == null) return null;
        return text
            .replace((char)0x93, '\"')
            .replace((char)0x94, '\"')
            .replace((char)0x92, '\'')
            .replace((char)0xA0, ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String imageToBase64(String imageUrl, String referer) throws IOException {
        Request request = new Request.Builder()
                .url(imageUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", referer)
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

    private static class SearchSource {
        final String platform;
        final String region;
        final String listUrl;

        SearchSource(String platform, String region, String listUrl) {
            this.platform = platform;
            this.region = region;
            this.listUrl = listUrl;
        }
    }
}

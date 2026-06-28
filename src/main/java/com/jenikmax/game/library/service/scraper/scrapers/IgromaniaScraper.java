package com.jenikmax.game.library.service.scraper.scrapers;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.JsoupHelper;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class IgromaniaScraper implements Scraper {

    private final ScraperConfig config;
    private final String type;
    private final JsoupHelper jsoupHelper;
    private static final String BASE_64_PREFIX = "data:image/jpeg;base64,";

    public IgromaniaScraper(ScraperConfig config, ConfigEncryptionService encryptionService,
                            JsoupHelper jsoupHelper) {
        this.config = config;
        this.jsoupHelper = jsoupHelper;
        this.type = config.getType();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            Map<String, Object> gameData = scrapeGameInfoJson(scrapInfo.getUrl());
            if (scrapInfo.isTitleAttr() && gameData.get("title") != null) {
                gameDto.setName(gameData.get("title").toString());
            }
            if (scrapInfo.isPosterAttr() && gameData.get("posterBase64") != null) {
                gameDto.setLogo(gameData.get("posterBase64").toString());
            }
            if (scrapInfo.isDescriptionAttr() && gameData.get("description") != null) {
                gameDto.setDescription(gameData.get("description").toString());
            }
            if (scrapInfo.isYearAttrAttr() && gameData.get("year") != null) {
                gameDto.setReleaseDate(gameData.get("year").toString());
            }
            if (scrapInfo.isGenresAttr() && gameData.get("genres") != null) {
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            if (scrapInfo.isScreensAttr() && gameData.get("screens") != null) {
                gameDto.setScreenshots((List<String>) gameData.get("screens"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Igromania scrape failed for URL: " + scrapInfo.getUrl(), e);
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto) {
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Map<String, Object> gameData = scrapeGameInfoJson(url);
            gameDto.setName(gameData.get("title").toString());
            gameDto.setLogo(gameData.get("posterBase64").toString());
            gameDto.setDescription(gameData.get("description").toString());
            gameDto.setReleaseDate(gameData.get("year").toString());
            gameDto.setGenres((List<String>) gameData.get("genres"));
            gameDto.setScreenshots((List<String>) gameData.get("screens"));
        } catch (Exception e) {
            throw new RuntimeException("Igromania scrape failed for URL: " + url, e);
        }
        return gameDto;
    }

    public Map<String, Object> scrapeGameInfoJson(String gameUrl) throws IOException {
        Map<String, Object> gameData = new HashMap<>();
        Document document = jsoupHelper.fetchDocument(gameUrl, config);
        String json = document.toString();
        String scriptBegin = "<script id=\"__NEXT_DATA__\" type=\"application/json\" crossorigin=\"anonymous\">";
        json = json.substring(json.indexOf(scriptBegin) + scriptBegin.length());
        json = json.substring(0, json.indexOf("</script>"));
        DocumentContext data = JsonPath.parse(json);

        Map<String, String> paths = config.getJsonPaths();
        if (paths == null) paths = Collections.emptyMap();

        String title = data.read(paths.getOrDefault("title",
                "$.props.initialStoreState.databaseElementPageStore.element.name"));
        gameData.put("title", title);

        String description = data.read(paths.getOrDefault("description",
                "$.props.initialStoreState.databaseElementPageStore.element.description"));
        gameData.put("description", description);

        String year = data.read(paths.getOrDefault("year",
                "$.props.initialStoreState.databaseElementPageStore.element.release_date.string"));
        gameData.put("year", year == null || year.isEmpty() ? "N/A" : year.substring(year.lastIndexOf(' ') + 1));

        String posterImageUrl = data.read(paths.getOrDefault("posterImage",
                "$.props.initialStoreState.databaseElementPageStore.element.image.origin"));
        String posterBase64 = imageToBase64(posterImageUrl);
        gameData.put("posterBase64", BASE_64_PREFIX + posterBase64);

        Set<String> genres = new HashSet<>();
        try {
            List<String> genresList = data.read(paths.getOrDefault("genres",
                    "$.props.initialStoreState.databaseElementPageStore.element.genres[*].slug"));
            Map<String, List<String>> mappings = config.getGenreMappings();
            if (mappings == null) mappings = Collections.emptyMap();
            for (String genre : genresList) {
                List<String> mapped = mappings.get(genre);
                if (mapped != null) {
                    genres.addAll(mapped);
                } else {
                    genres.add(genre);
                }
            }
        } catch (Exception e) {
            // genres optional
        }
        gameData.put("genres", new ArrayList<>(genres));

        List<String> screensList = new ArrayList<>();
        try {
            String screensPath = paths.getOrDefault("screenshots",
                    "$.props.initialStoreState.databaseElementPageStore.screenshots.items.results[*].file.origin");
            List<String> imageUrlList = data.read(screensPath);
            int max = config.getMaxScreenshots();
            for (int i = 0; i < imageUrlList.size() && i < max; i++) {
                try {
                    screensList.add(BASE_64_PREFIX + imageToBase64(imageUrlList.get(i)));
                } catch (Exception e) {
                    // skip failed screenshot
                }
            }
        } catch (Exception e) {
            // screens optional
        }
        gameData.put("screens", screensList);

        return gameData;
    }

    public String imageToBase64(String imageUrl) throws IOException {
        byte[] imageBytes = jsoupHelper.fetchBytes(imageUrl, config);
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}

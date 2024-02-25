package com.jenikmax.game.library.service.scraper.scrapers;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class PlaygroundScraper implements Scraper {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(scrapInfo.getUrl());
            if(scrapInfo.isTitleAttr()){
                gameDto.setName(gameData.get("title").toString());
            }
            if(scrapInfo.isYearAttrAttr()){
                Map<String, String> release = (Map<String,String>) gameData.get("releaseDates");
                for(String platforms : release.keySet()){
                    if(platforms.toUpperCase().contains(gameDto.getPlatform().toUpperCase())) gameDto.setReleaseDate(release.get(platforms));
                }
            }
            if(scrapInfo.isPosterAttr()){
                gameDto.setLogo(BASE_64_PREFIX + gameData.get("posterBase64"));
            }
            if(scrapInfo.isDescriptionAttr()){
                gameDto.setDescription(gameData.get("description").toString());
            }
            if(scrapInfo.isGenresAttr()){
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            if(scrapInfo.isScreensAttr()){
                gameDto.setScreenshots(getScreenshots(scrapInfo.getUrl()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(url);
            gameDto.setName(gameData.get("title").toString());
            gameDto.setLogo(BASE_64_PREFIX + gameData.get("posterBase64"));
            gameDto.setDescription(gameData.get("description").toString());
            Map<String, String> release = (Map<String,String>) gameData.get("releaseDates");
            for(String platforms : release.keySet()){
                if(platforms.toUpperCase().contains(gameDto.getPlatform().toUpperCase())) gameDto.setReleaseDate(release.get(platforms));
            }
            gameDto.setGenres((List<String>) gameData.get("genres"));
            gameDto.setScreenshots(getScreenshots(url));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    @Override
    public GameDto scrap(GameDto gameDto) {
        return gameDto;
    }






    public Map<String, Object> scrapeGameInfo(String gameUrl) throws IOException {
        Map<String, Object> gameData = new HashMap<>();
        Document document = Jsoup.connect(gameUrl).get();

        // Get game title
        String title = document.select("h1.gp-game-title").text();
        gameData.put("title", title);

        // Get game description
        String description = document.select("div.description-wrapper > p").text();
        gameData.put("description", description);

        // Get game poster as base64
        String posterImageUrl = document.select("div.gp-game-cover > div.cover-link > img").attr("src");
        String posterBase64 = imageToBase64(posterImageUrl.substring(0,posterImageUrl.lastIndexOf('?')));
        gameData.put("posterBase64", posterBase64);

        // Get release dates
        Map<String, String> releaseDates = new HashMap<>();
        for (Element platformElement : document.select("div.release-item")) {
            String releaseDate = platformElement.select("span.date").text();
            if(releaseDate.length() > 4) releaseDate = releaseDate.substring(releaseDate.lastIndexOf('.') + 1);
            String platform = platformElement.select("span.platform").text();
            releaseDates.put(platform, releaseDate);
        }
        gameData.put("releaseDates", releaseDates);

        // Get genres
        List<String> genres = new ArrayList<>();
        for (Element genresElement : document.select("div.genres > a")) {
            genres.add(genresElement.attr("href").replaceAll("/games/","").replaceAll("-","_"));
        }
        //String genres = document.select("div.genres > a").text();
        gameData.put("genres", genres);

        return gameData;
    }

    private List<String> getScreenshots(String gameUrl) throws IOException {
        List<String> result = new ArrayList<>();
        String screenUrl = gameUrl.substring(0,gameUrl.lastIndexOf('/')) + "/gallery" + gameUrl.substring(gameUrl.lastIndexOf('/'));
        Document document = Jsoup.connect(screenUrl).get();
        Elements screensA = document.select("div.gallery-item > a");
        for(int i = 0 ; i <= screensA.size() && i <= 20 ; i++ ){ // добавлен лимит на 20 изображений
            result.add(BASE_64_PREFIX + imageToBase64(screensA.get(i).attr("href")));
        }
        return result;
    }


    public String imageToBase64(String imageUrl) throws IOException {
        byte[] imageBytes = Jsoup.connect(imageUrl).ignoreContentType(true).execute().bodyAsBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    @Override
    public String getType() {
        return "playground";
    }





}

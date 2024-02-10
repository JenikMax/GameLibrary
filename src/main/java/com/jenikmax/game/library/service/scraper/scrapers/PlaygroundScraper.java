package com.jenikmax.game.library.service.scraper.scrapers;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class PlaygroundScraper implements Scraper {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(url);
            gameDto.setName(gameData.get("title").toString());
            gameDto.setLogo(BASE_64_PREFIX + gameData.get("posterBase64"));
            gameDto.setDescription(gameData.get("description").toString());
            gameDto.setReleaseDate(((Map<String,String>)gameData.get("releaseDates")).get(gameDto.getPlatform().toUpperCase()));
            gameDto.setGenres((List<String>) gameData.get("genres"));
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
        String posterBase64 = imageToBase64(posterImageUrl);
        gameData.put("posterBase64", posterBase64);

        // Get release dates
        Map<String, String> releaseDates = new HashMap<>();
        for (Element platformElement : document.select("div.release-item")) {
            String platform = platformElement.select("span.date").text();
            String releaseDate = platformElement.select("span.platform").text();
            releaseDates.put(platform, releaseDate);
        }
        gameData.put("releaseDates", releaseDates);

        // Get genres
        List<String> genres = new ArrayList<>();
        for (Element genresElement : document.select("div.genres > a")) {
            genres.add(genresElement.attr("href").replaceAll("/games/",""));
        }
        //String genres = document.select("div.genres > a").text();
        gameData.put("genres", genres);

        return gameData;
    }

    public String imageToBase64(String imageUrl) throws IOException {
        byte[] imageBytes = Jsoup.connect(imageUrl).ignoreContentType(true).execute().bodyAsBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public void scrap(){
        try (WebClient webClient = new WebClient()) {
            // Включение поддержки JavaScript для HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);

            // Получение страницы поиска
            HtmlPage searchPage = webClient.getPage("https://www.playground.ru/search/");

            // Получение формы поиска по названию игры
            HtmlForm searchForm = searchPage.getFirstByXPath("//form[@id='search_block']");
            if (searchForm == null) {
                System.out.println("Форма поиска не найдена.");
                return;
            }

            // Ввод названия игры в строку поиска
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите название игры: ");
            String gameName = scanner.nextLine();

            // Заполнение строки поиска и отправка запроса
            HtmlTextInput searchInput = searchForm.getInputByName("q");
            searchInput.setValueAttribute(gameName);
            HtmlPage searchResultsPage = searchForm.getInputByValue("Искать").click();

            // Получение ссылки на страницу игры из результатов поиска (первая ссылка)
            HtmlAnchor gamePageLink = searchResultsPage.getFirstByXPath("//a[contains(@class, 'search-item__title-link')]");
            if (gamePageLink == null) {
                System.out.println("Страница игры не найдена.");
                return;
            }

            // Переход на страницу игры
            HtmlPage gamePage = gamePageLink.click();

            // Получение данных игры
            String title = gamePage.getTitleText();
            HtmlImage coverImage = gamePage.getFirstByXPath("//img[contains(@class, 'cover-image')]");
            String coverImageUrl = coverImage.getSrcAttribute();
            String description = gamePage.getFirstByXPath("//div[contains(@class, 'game-description')]//p").toString();

            // Вывод данных на консоль
            System.out.println("Название: " + title);
            System.out.println("Изображение обложки: " + coverImageUrl);
            System.out.println("Описание: " + description);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public String getType() {
        return "playground";
    }


    public String convertPGGenres(String genre){
        if("".contains(genre)){
            return "";
        }
        return "N/A";
    }





}

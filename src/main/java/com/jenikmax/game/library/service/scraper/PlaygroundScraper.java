package com.jenikmax.game.library.service.scraper;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import java.util.Scanner;

public class PlaygroundScraper {

    public void scrap(){
        try (WebClient webClient = new WebClient()) {
            // Включение поддержки JavaScript для HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);

            // Получение страницы поиска
            HtmlPage searchPage = webClient.getPage("https://www.playground.ru/search");

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
}

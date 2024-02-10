package com.jenikmax.game.library.service.scraper.scrapers;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Scanner;

public class WorldArtScraper {

    public void scrap(){
        try (WebClient webClient = new WebClient()) {
            // Включение поддержки JavaScript для HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);

            // Получение страницы с использованием HtmlUnit
            HtmlPage searchPage = webClient.getPage("http://www.world-art.ru");

            // Получение формы поиска по названию игры
            HtmlForm searchForm = searchPage.getFirstByXPath("//form[@id='searchform']");
            if (searchForm == null) {
                System.out.println("Форма поиска не найдена.");
                return;
            }

            // Ввод названия игры в строку поиска
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите название игры: ");
            String gameName = scanner.nextLine();

            // Заполнение строки поиска
            searchForm.getInputByName("searchtext").setValueAttribute(gameName);
            HtmlSubmitInput submitBtn = searchForm.getInputByName("dosearch");

            // Отправка запроса на поиск
            HtmlPage searchResultsPage = submitBtn.click();

            // Получение HTML-кода страницы
            String html = searchResultsPage.asXml();

            // Создание объекта Document с использованием Jsoup
            Document doc = Jsoup.parse(html);

            // Парсинг и обработка данных с помощью Jsoup
            Elements newsElements = doc.select(".foundresult");

            for (Element element : newsElements) {
                String title = element.selectFirst(".RUbreaking").text();
                System.out.println("Заголовок: " + title);
                System.out.println("----------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

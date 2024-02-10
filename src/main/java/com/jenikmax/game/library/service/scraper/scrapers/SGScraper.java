package com.jenikmax.game.library.service.scraper.scrapers;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Scanner;


public class SGScraper {

    public void scrap(){
        try (WebClient webClient = new WebClient()) {
            // Включение поддержки JavaScript для HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);

            // Получение страницы поиска
            HtmlPage searchPage = webClient.getPage("https://stopgame.ru/search?search=");

            // Ввод названия игры в строку поиска
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите название игры: ");
            String gameName = scanner.nextLine();

            // Заполнение строки поиска
            HtmlForm searchForm = searchPage.getFirstByXPath("//form[@class='searchtop search-form']");
            if (searchForm == null) {
                System.out.println("Форма поиска не найдена.");
                return;
            }
            searchForm.getInputByName("q").setValueAttribute(gameName);

            // Отправка запроса на поиск
            HtmlPage searchResultsPage = searchForm.getInputByValue("Искать").click();

            // Получение HTML-кода страницы
            String html = searchResultsPage.asXml();

            // Создание объекта Document с использованием Jsoup
            Document doc = Jsoup.parse(html);

            // Парсинг и обработка данных с помощью Jsoup
            Elements newsElements = doc.select(".tiles-item");

            for (Element element : newsElements) {
                String title = element.selectFirst(".tiles-item-title").text();
                String imageURL = element.selectFirst(".tiles-item-pic").absUrl("data-background-image");

                System.out.println("Заголовок: " + title);
                System.out.println("URL изображения: " + imageURL);
                System.out.println("----------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

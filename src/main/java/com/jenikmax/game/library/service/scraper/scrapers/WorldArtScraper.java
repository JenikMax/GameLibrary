package com.jenikmax.game.library.service.scraper.scrapers;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Map;

public class WorldArtScraper implements Scraper {

    private final ScraperConfig config;
    private final String type;

    public WorldArtScraper(ScraperConfig config) {
        this.config = config;
        this.type = config.getType();
    }

    @Override
    public String getType() { return type; }

    @Override
    public GameDto scrap(GameDto gameDto) { return gameDto; }

    @Override
    public GameDto scrap(GameDto gameDto, String url) {
        return scrap(gameDto);
    }

    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        String searchQuery = extractSearchQuery(scrapInfo, gameDto);
        if (searchQuery == null) return gameDto;

        try (WebClient webClient = new WebClient()) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setTimeout(config.getTimeoutMs());

            Map<String, String> sel = config.getCssSelectors();
            if (sel == null) sel = java.util.Collections.emptyMap();

            String baseUrl = config.getBaseUrl() != null ? config.getBaseUrl() : "http://www.world-art.ru";
            HtmlPage searchPage = webClient.getPage(baseUrl);

            String searchFormId = sel.getOrDefault("searchForm", "searchform");
            HtmlForm searchForm = searchPage.getFirstByXPath("//form[@id='" + searchFormId + "']");
            if (searchForm == null) return gameDto;

            String searchInputName = sel.getOrDefault("searchInput", "searchtext");
            searchForm.getInputByName(searchInputName).setValueAttribute(searchQuery);

            String submitInputName = sel.getOrDefault("searchSubmit", "dosearch");
            HtmlSubmitInput submitBtn = searchForm.getInputByName(submitInputName);
            HtmlPage searchResultsPage = submitBtn.click();

            String html = searchResultsPage.asXml();
            Document doc = Jsoup.parse(html);

            String resultSel = sel.getOrDefault("resultItem", ".foundresult");
            String titleSel = sel.getOrDefault("resultTitle", ".RUbreaking");

            Elements newsElements = doc.select(resultSel);
            if (!newsElements.isEmpty()) {
                Element first = newsElements.first();
                Element titleElement = first.selectFirst(titleSel);
                if (titleElement != null && scrapInfo.isTitleAttr()) {
                    gameDto.setName(titleElement.text());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractSearchQuery(ScrapInfo scrapInfo, GameDto gameDto) {
        if (scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty()) {
            return scrapInfo.getUrl().trim();
        }
        return gameDto.getName();
    }
}

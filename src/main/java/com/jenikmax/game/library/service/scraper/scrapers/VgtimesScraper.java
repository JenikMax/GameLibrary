package com.jenikmax.game.library.service.scraper.scrapers;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
public class VgtimesScraper implements Scraper {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";


    @Override
    public String getType() {
        return "vgtimes";
    }


    @Override
    public GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo) {
        try {
            Map<String, Object> gameData = scrapeGameInfo(scrapInfo.getUrl());
            if(scrapInfo.isTitleAttr()){
                gameDto.setName(gameData.get("title").toString());
            }
            if(scrapInfo.isPosterAttr()){
                gameDto.setLogo(gameData.get("posterBase64").toString());
            }
            if(scrapInfo.isDescriptionAttr()){
                gameDto.setDescription(gameData.get("description").toString());
            }
            //if(scrapInfo.isYearAttrAttr()){
            //    gameDto.setReleaseDate(gameData.get("year").toString());
            //}
            if(scrapInfo.isGenresAttr()){
                gameDto.setGenres((List<String>) gameData.get("genres"));
            }
            if(scrapInfo.isScreensAttr()){
                gameDto.setScreenshots((List<String>)gameData.get("screens"));
            }
        } catch (IOException e) {
            e.printStackTrace();
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
            Map<String, Object> gameData = scrapeGameInfo(url);
            gameDto.setName(gameData.get("title").toString());
            gameDto.setLogo(gameData.get("posterBase64").toString());
            gameDto.setDescription(gameData.get("description").toString());
            gameDto.setReleaseDate(gameData.get("year").toString());
            gameDto.setGenres((List<String>) gameData.get("genres"));
            gameDto.setScreenshots((List<String>)gameData.get("screens"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    public Map<String, Object> scrapeGameInfo(String gameUrl) throws IOException {
        Map<String, Object> gameData = new HashMap<>();
        Document document = Jsoup.connect(gameUrl).get();

        // Get game title
        String title = document.select("div.game_header > h1").text();
        gameData.put("title", title);

        // Get game description
        Elements pElements = document.select(".secondary.des .game_story.description p");
        String description = "";
        if (pElements.size() > 0) {
            Element firstParagraph = pElements.first();
            description = firstParagraph.text();
        }
        //String description = document.select(".secondary.des .game_story.description p:first-of-type").text();
        gameData.put("description", description);

        // Get game poster as base64
        String posterImageUrl = "https://vgtimes.ru" + document.select("div.header_info .img_holder.hasslider img").attr("data-src");
        String posterBase64 = BASE_64_PREFIX + imageToBase64(posterImageUrl);
        gameData.put("posterBase64", posterBase64);

        // Get release dates

        // Get genres
        Set<String> genres = new HashSet<>();
        for (Element genresElement : document.select("div.gres > a")) {
            converGenre(genresElement.attr("href").replaceAll("/games/genres/","").replaceAll("-","_").replaceAll("/",""),genres);
        }
        gameData.put("genres",  new ArrayList<>(genres));

        // Get game screens
        List<String> screensList = new ArrayList<>();
        Elements screensA = document.select("div.game_media li");
        for(int i = 0 ; i < screensA.size() && i < 20 ; i++ ){ // добавлен лимит на 20 изображений
            if(!screensA.get(i).attr("data-src").contains("youtube")){
                screensList.add(BASE_64_PREFIX + imageToBase64(screensA.get(i).attr("data-src")));
            }
        }
        gameData.put("screens", screensList);

        return gameData;
    }


    private void converGenre(String genre, Set<String> genres){
        switch (genre) {
            case "ekshen":
                genres.add("action");
                break;
            case "survive":
                genres.add("survival");
                break;
            case "casual":
                // NO MATCH
                break;
            case "survival-horror":
                genres.add("survival");
                genres.add("horror");
                break;
            case "citybuilder":
                genres.add("construction");
                break;
            case "dungeons":
                genres.add("dungeons");
                break;
            case "comedy":
                genres.add("humour");
                break;
            case "exploration":
                // NO MATCH
                break;
            case "narration":
                // NO MATCH
                break;
            case "openworld":
                genres.add("open_world");
                break;
            case "trivia":
                genres.add("logic");
                break;
            case "replayvalue":
                // NO MATCH
                break;
            case "retro":
                genres.add("retro");
                break;
            case "hidden-object":
                genres.add("point_click");
                break;
            case "fantasy":
                genres.add("fantasy");
                break;
            case "oldschool":
                // NO MATCH
                break;
            case "medieval":
                genres.add("medieval");
                break;
            case "space":
                genres.add("space");
                break;
            case "building":
                genres.add("construction");
                break;
            case "crafting":
                // NO MATCH
                break;
            case "proceduralgeneration":
                // NO MATCH
                break;
            case "zombies":
                genres.add("zombie");
                break;
            case "jrpg":
                genres.add("jrpg");
                break;
            case "demons":
                // NO MATCH
                break;
            case "clicker":
                // NO MATCH
                break;
            case "detective":
                // NO MATCH
                break;
            case "metroidvania":
                genres.add("metroidvaniia");
                break;
            case "parkour":
                // NO MATCH
                break;
            case "grandstrategy":
                genres.add("strategy");
                break;
            case "battle-royale":
                // NO MATCH
                break;
            case "gothic":
                // NO MATCH
                break;
            case "moba":
                genres.add("moba");
                break;
            case "remake":
                // NO MATCH
                break;
            case "kki":
                genres.add("kki");
                break;
            case "fantastika-futurizm":
                genres.add("sci_fi");
                break;
            case "fantastika":
                genres.add("sci_fi");
                break;
            case "onlayn":
                genres.add("mmo");
                break;
            case "kvest-priklyuchenie":
                genres.add("quest");
                genres.add("adventure");
                break;
            case "point-click":
                genres.add("point_click");
                break;
            case "text-quest":
                genres.add("quest");
                break;
            case "runner":
                // NO MATCH
                break;
            case "mistika":
                genres.add("mistika");
                break;
            case "rogalik":
                genres.add("roguelike");
                break;
            case "virtualnaya-realnost":
                genres.add("vr");
                break;
            case "scifi":
                genres.add("sci_fi");
                break;
            case "kooperativ":
                genres.add("coop");
                break;
            case "indi":
                genres.add("indie");
                break;
            case "mini-igra-brauzernaya":
                // NO MATCH
                break;
            case "testovaya-programma":
                // NO MATCH
                break;
            case "uslovno-besplatnaya":
                // NO MATCH
                break;
            case "taktika":
                genres.add("tactics");
                break;
            case "stimpank":
                genres.add("steampunk");
                break;
            case "emulator":
                // NO MATCH
                break;
            case "istoricheskaya":
                genres.add("istoriia");
                break;
            case "tryuki":
                // NO MATCH
                break;
            case "karty-igralnye-kosti":
                genres.add("card");
                break;
            case "kazino":
                // NO MATCH
                break;
            case "sandbox":
                genres.add("sandbox");
                break;
            case "igra-dlya-vzroslyh":
                genres.add("adult");
                break;
            case "fantasy-middleage":
                genres.add("fantasy");
                break;
            case "vertolety":
                genres.add("flight");
                break;
            case "golovolomka":
                genres.add("logic");
                break;
            case "sportivnaya-igra":
                genres.add("sport");
                break;
            case "anime-manga":
                genres.add("anime");
                break;
            case "pinbol":
                // NO MATCH
                break;
            case "motogonki":
                genres.add("racing");
                break;
            case "vizualnyy-roman":
                genres.add("vizualnaia_novella");
                break;
            case "arkanoid":
                // NO MATCH
                break;
            case "interaktivnoe-kino":
                genres.add("interaktivnoe_kino");
                break;
            case "arkada":
                genres.add("arcade");
                break;
            case "stels":
                genres.add("stealth");
                break;
            case "fayting":
                genres.add("fighting");
                break;
            case "mmo":
                genres.add("mmo");
                break;
            case "rubi-i-rezh":
                // NO MATCH
                break;
            case "horror":
                genres.add("horror");
                break;
            case "priklyuchenie":
                genres.add("adventure");
                break;
            case "rolevaya-igra":
                genres.add("rpg");
                break;
            case "shuter":
                genres.add("shooter");
                break;
            case "gonki-vozhdenie":
                genres.add("racing");
                break;
            case "strategiya":
                genres.add("strategy");
                break;
            case "slesher":
                genres.add("slesher");
                break;
            case "biznes-menedzhment":
                genres.add("manager");
                break;
            case "obuchayuschaya-igra":
                // NO MATCH
                break;
            case "platformer":
                genres.add("platform");
                break;
            case "muzykalnaya-igra":
                genres.add("music");
                break;
            case "pobedi-ih-vseh":
                genres.add("beat_em_up");
                break;
            default:
                genres.add("other");
        }
    }


    public String imageToBase64(String imageUrl) throws IOException {
        byte[] imageBytes = Jsoup.connect(imageUrl).ignoreContentType(true).execute().bodyAsBytes();
        return Base64.getEncoder().encodeToString(imageBytes);
    }


}

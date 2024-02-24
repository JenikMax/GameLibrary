package com.jenikmax.game.library.service.scraper.scrapers;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class IgromaniaScraper implements Scraper {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";


    @Override
    public String getType() {
        return "igromania";
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
            gameDto.setScreenshots((List<String>)gameData.get("screens"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    public Map<String, Object> scrapeGameInfoJson(String gameUrl) throws IOException {
        Map<String, Object> gameData = new HashMap<>();
        Document document = Jsoup.connect(gameUrl).get();
        String json = document.toString();
        String scriptBegin = "<script id=\"__NEXT_DATA__\" type=\"application/json\" crossorigin=\"anonymous\">";
        json = json.substring(json.indexOf(scriptBegin)+scriptBegin.length());
        json = json.substring(0,json.indexOf("</script>"));
        //json = json.substring(json.indexOf(scriptBegin)+scriptBegin.length()).substring(0,json.indexOf("</script>"));
        //System.out.println("json - " + json);
        DocumentContext data = JsonPath.parse(json);
        // Get game title
        String title = data.read("$.props.initialStoreState.databaseElementPageStore.element.name");
        gameData.put("title", title);
        // Get game description
        String description = data.read("$.props.initialStoreState.databaseElementPageStore.element.description");
        gameData.put("description", description);
        // Get game year
        String year = data.read("$.props.initialStoreState.databaseElementPageStore.element.release_date.string");
        gameData.put("year", year==null || year.isEmpty() ? "N/A" : year.substring(year.lastIndexOf(' ')+1));
        // Get game poster as base64
        String posterImageUrl = data.read("$.props.initialStoreState.databaseElementPageStore.element.image.origin");
        String posterBase64 = imageToBase64(posterImageUrl);
        gameData.put("posterBase64", BASE_64_PREFIX + posterBase64);
        // Get genres
        Set<String> genres = new HashSet<>();
        List<String> genresList = data.read("$.props.initialStoreState.databaseElementPageStore.element.genres[*].slug");
        for(String genre : genresList){
            converGenre(genre,genres);
        }
        gameData.put("genres", new ArrayList<>(genres));
        // Get game screens
        List<String> screensList = new ArrayList<>();
        List<String> imageUrlList = data.read("$.props.initialStoreState.databaseElementPageStore.screenshots.items.results[*].file.origin");
        for(int i = 0 ; i <= imageUrlList.size() && i <= 20 ; i++ ){ // добавлен лимит на 20 изображений
            try{
                screensList.add(BASE_64_PREFIX + imageToBase64(imageUrlList.get(i)));
            }
            catch (Exception e){
                //
            }
        }
        gameData.put("screens", screensList);
        return gameData;
    }


    private void converGenre(String genre, Set<String> genres){
       switch (genre) {
           case "beat-em-up":
               genres.add("beat_em_up");
               break;
           case "soulslike":
               genres.add("soulslike");
               break;
           case "rolevoj-ekshen":
               genres.add("action");
               genres.add("rpg");
               break;
           case "fps":
               genres.add("first_person");
               genres.add("shooter");
               break;
           case "hack-and-slash":
               genres.add("hack_and_slash");
               break;
           case "immersive-sim":
               genres.add("immersive_sim");
               break;
           case "mmo":
               genres.add("mmo");
               break;
           case "moba":
               genres.add("moba");
               break;
           case "point-click":
               genres.add("point_click");
               break;
           case "roguelike":
               genres.add("roguelike");
               break;
           case "td":
               genres.add("tower_defence");
               break;
           case "tps":
               genres.add("third_person");
               genres.add("shooter");
               break;
           case "arkada":
               genres.add("arcade");
               break;
           case "vizualnaia-novella":
               genres.add("vizualnaia_novella");
               break;
           case "vyzhivanie":
               genres.add("survival");
               break;
           case "gonki":
               genres.add("racing");
               break;
           case "igra-dlia-vzroslykh":
               genres.add("adult");
               break;
           case "interaktivnoe-kino":
               genres.add("interaktivnoe_kino");
               break;
           case "istoriia":
               genres.add("istoriia");
               break;
           case "kvest":
               genres.add("quest");
               break;
           case "kki":
               genres.add("kki");
               break;
           case "kooperativ":
               genres.add("coop");
               break;
           case "menedzher":
               genres.add("manager");
               break;
           case "metroidvaniia":
               genres.add("metroidvaniia");
               break;
           case "muzyka":
               genres.add("music");
               break;
           case "nastolnaia-igra":
               genres.add("board_game");
               break;
           case "pazzl":
               genres.add("pazzl");
               break;
           case "platformer":
               genres.add("platform");
               break;
           case "poshagovaia-strategiia":
               genres.add("strategy");
               genres.add("turn_based");
               break;
           case "prikliuchenie":
               genres.add("adventure");
               break;
           case "rolevaia-igra":
               genres.add("rpg");
               break;
           case "simuliator":
               genres.add("simulators");
               break;
           case "simuliator-khodby":
               genres.add("other_simulators");
               break;
           case "sport":
               genres.add("sport");
               break;
           case "strategiia":
               genres.add("strategy");
               break;
           case "strategiia-v-realnom-vremeni":
               genres.add("rts");
               break;
           case "taktika":
               genres.add("tactics");
               break;
           case "faiting":
               genres.add("fighting");
               break;
           case "khorror":
               genres.add("horror");
               break;
           case "shuter":
               genres.add("shooter");
               break;
           case "ekshen":
               genres.add("action");
               break;
           default:
               genres.add("other");
       }
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



    public void scrap(){
        try (WebClient webClient = new WebClient()) {
            // Включение поддержки JavaScript для HtmlUnit
            webClient.getOptions().setJavaScriptEnabled(true);

            // Получение страницы поиска
            HtmlPage searchPage = webClient.getPage("https://www.igromania.ru/search/?q=");

            // Ввод названия игры в строку поиска
            Scanner scanner = new Scanner(System.in);
            System.out.print("Введите название игры: ");
            String gameName = scanner.nextLine();

            // Заполнение строки поиска
            HtmlForm searchForm = searchPage.getFirstByXPath("//form[@name='search-block']");
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
            Elements newsElements = doc.select(".gel-layout > .list-preview");

            for (Element element : newsElements) {
                String title = element.selectFirst("a").text();
                String imageURL = element.selectFirst("img").absUrl("src");

                System.out.println("Заголовок: " + title);
                System.out.println("URL изображения: " + imageURL);
                System.out.println("----------------------------");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

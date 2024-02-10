package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TheGameDBScraper {

    private static final String BASE_URL = "https://api.thegamesdb.net/v1/Games/ByGameName";
    private static final String API_KEY = "YOUR_API_KEY";

    public void scrap(){
        String gameName = "The Witcher 3: Wild Hunt";

        try {
            // Создание HTTP-клиента
            OkHttpClient client = new OkHttpClient();

            // Формирование запроса к TheGamesDB.net API
            String requestUrl = String.format(
                    "%s?apikey=%s&filter[name]=%s", BASE_URL, API_KEY, gameName);
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .get()
                    .build();

            // Отправка запроса и получение ответа
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();

            // Обработка ответа в формате JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseData);

            // Получение данных об игре из ответа
            String name = jsonNode.get("data").get("games").get(0).get("game_title").asText();
            String releaseDate = jsonNode.get("data").get("games").get(0).get("release_date").asText();
            String overview = jsonNode.get("data").get("games").get(0).get("overview").asText();
            String imageUrl = jsonNode.get("data").get("games").get(0).get("thumb").asText();

            // Вывод полученных данных
            System.out.println("Название: " + name);
            System.out.println("Дата выпуска: " + releaseDate);
            System.out.println("Описание: " + overview);
            System.out.println("URL изображения: " + imageUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

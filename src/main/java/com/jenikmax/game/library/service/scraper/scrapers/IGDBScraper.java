package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

public class IGDBScraper {

    private static final String BASE_URL = "https://api.igdb.com/v4/games";
    private static final String CLIENT_ID = "YOUR_CLIENT_ID";
    private static final String ACCESS_TOKEN = "YOUR_ACCESS_TOKEN";

    public void scrap(){
        String gameName = "The Witcher 3: Wild Hunt";

        try {
            // Создание HTTP-клиента
            OkHttpClient client = new OkHttpClient();

            // Формирование запроса к IGDB API
            String query = String.format(
                    "search \"%s\"; fields name,summary,cover; limit 1;", gameName);
            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .header("Client-ID", CLIENT_ID)
                    .header("Authorization", "Bearer " + ACCESS_TOKEN)
                    .post(RequestBody.create(MediaType.parse("text/plain"), query))
                    .build();

            // Отправка запроса и получение ответа
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();

            // Обработка ответа в формате JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseData);

            // Получение данных об игре из ответа
            String name = jsonNode.get(0).get("name").asText();
            String summary = jsonNode.get(0).get("summary").asText();
            String coverUrl = jsonNode.get(0).get("cover").get("url").asText();

            // Вывод полученных данных
            System.out.println("Название: " + name);
            System.out.println("Описание: " + summary);
            System.out.println("Ссылка на обложку: " + coverUrl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

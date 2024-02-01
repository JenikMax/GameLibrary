package com.jenikmax.game.library.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SteamScraper {

    //Получение информации о приложении (игре) по его ID
    private static final String STEAM_API_KEY = "YOUR_API_KEY";

    public void scrap(){
        try {
            OkHttpClient client = new OkHttpClient();
            String appId = "1085660";

            // Формирование запроса к Steam Web API
            String requestUrl = String.format("https://api.steampowered.com/ISteamApps/GetAppDetails/v1/?appid=%s&key=%s", appId, STEAM_API_KEY);
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
            JsonNode gameInfo = jsonNode.get("applist").get("apps").get(0);
            String appName = gameInfo.get("name").asText();
            String appDescription = gameInfo.get("detailed_description").asText();

            // Вывод полученных данных
            System.out.println("Название: " + appName);
            System.out.println("Описание: " + appDescription);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

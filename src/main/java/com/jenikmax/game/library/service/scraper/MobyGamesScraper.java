package com.jenikmax.game.library.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MobyGamesScraper {

    //YOUR_API_KEY на ваше реальное значение ключа доступа к API MobyGames.
    private static final String API_KEY = "YOUR_API_KEY";

   public void scrap(){
       try {
           OkHttpClient client = new OkHttpClient();
           String gameId = "dune"; // Замените на идентификатор игры

           // Формирование запроса к API MobyGames
           String requestUrl = String.format("https://api.mobygames.com/v1/games/%s?api_key=%s", gameId, API_KEY);
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
           String title = jsonNode.get("title").asText();
           String releaseDate = jsonNode.get("release_date").asText();
           String genre = jsonNode.get("genre").asText();
           String platform = jsonNode.get("platform").asText();

           // Вывод полученных данных
           System.out.println("Название: " + title);
           System.out.println("Дата выпуска: " + releaseDate);
           System.out.println("Жанр: " + genre);
           System.out.println("Платформа: " + platform);

       } catch (Exception e) {
           e.printStackTrace();
       }
   }


}

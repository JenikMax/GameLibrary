package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class MobyGamesScraper implements Scraper {

    private final ScraperConfig config;
    private final ConfigEncryptionService encryptionService;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public MobyGamesScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
        this.config = config;
        this.encryptionService = encryptionService;
        this.type = config.getType();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
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
        String gameId = extractGameId(scrapInfo.getUrl());
        if (gameId == null) return gameDto;

        try {
            String apiKey = encryptionService.decrypt(
                    config.getEncryptedApiKey() != null ? config.getEncryptedApiKey() : "");
            String requestUrl = String.format("%s/%s?api_key=%s", config.getApiUrl(), gameId, apiKey);
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseData);

            if (scrapInfo.isTitleAttr() && jsonNode.has("title")) {
                gameDto.setName(jsonNode.get("title").asText());
            }
            if (scrapInfo.isDescriptionAttr() && jsonNode.has("description")) {
                gameDto.setDescription(jsonNode.get("description").asText());
            }
            if (scrapInfo.isYearAttrAttr() && jsonNode.has("release_date")) {
                gameDto.setReleaseDate(jsonNode.get("release_date").asText());
            }
            if (scrapInfo.isGenresAttr() && jsonNode.has("genre")) {
                gameDto.setGenres(java.util.Collections.singletonList(jsonNode.get("genre").asText()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractGameId(String url) {
        if (url == null) return null;
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }
}

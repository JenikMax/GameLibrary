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

public class SteamScraper implements Scraper {

    private final ScraperConfig config;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public SteamScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
        this.config = config;
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
        String appId = extractAppId(scrapInfo.getUrl());
        if (appId == null) return gameDto;

        try {
            String requestUrl = String.format("%s?appid=%s&key=%s",
                    config.getApiUrl(), appId, "1"); // format param, key comes from config
            // Steam API v1 doesn't require key for appdetails, but we try with key anyway
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseData);
            JsonNode appData = jsonNode.get(appId);

            if (appData != null && appData.has("data")) {
                JsonNode data = appData.get("data");
                if (scrapInfo.isTitleAttr() && data.has("name")) {
                    gameDto.setName(data.get("name").asText());
                }
                if (scrapInfo.isDescriptionAttr() && data.has("short_description")) {
                    gameDto.setDescription(data.get("short_description").asText());
                }
                if (scrapInfo.isPosterAttr() && data.has("header_image")) {
                    gameDto.setLogo(data.get("header_image").asText());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractAppId(String url) {
        if (url == null) return null;
        // https://store.steampowered.com/app/1085660/
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("app".equals(parts[i])) return parts[i + 1];
        }
        return null;
    }
}

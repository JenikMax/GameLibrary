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

public class TheGameDBScraper implements Scraper {

    private final ScraperConfig config;
    private final ConfigEncryptionService encryptionService;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public TheGameDBScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
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
        String gameName = extractGameName(scrapInfo, gameDto);
        if (gameName == null) return gameDto;

        try {
            String apiKey = encryptionService.decrypt(
                    config.getEncryptedApiKey() != null ? config.getEncryptedApiKey() : "");
            String requestUrl = String.format("%s?apikey=%s&filter[name]=%s",
                    config.getApiUrl(), apiKey, gameName.replace(" ", "%20"));
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseData);

            if (jsonNode.has("data") && jsonNode.get("data").has("games")
                    && jsonNode.get("data").get("games").size() > 0) {
                JsonNode game = jsonNode.get("data").get("games").get(0);
                if (scrapInfo.isTitleAttr() && game.has("game_title")) {
                    gameDto.setName(game.get("game_title").asText());
                }
                if (scrapInfo.isDescriptionAttr() && game.has("overview")) {
                    gameDto.setDescription(game.get("overview").asText());
                }
                if (scrapInfo.isYearAttrAttr() && game.has("release_date")) {
                    gameDto.setReleaseDate(game.get("release_date").asText());
                }
                if (scrapInfo.isPosterAttr() && game.has("thumb")) {
                    gameDto.setLogo(game.get("thumb").asText());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractGameName(ScrapInfo scrapInfo, GameDto gameDto) {
        if (scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty()) {
            String[] parts = scrapInfo.getUrl().split("/");
            return parts[parts.length - 1].replace("-", " ").replace("_", " ");
        }
        return gameDto.getName();
    }
}

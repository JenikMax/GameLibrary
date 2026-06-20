package com.jenikmax.game.library.service.scraper.scrapers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.service.scraper.ConfigEncryptionService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

public class IGDBScraper implements Scraper {

    private final ScraperConfig config;
    private final ConfigEncryptionService encryptionService;
    private final String type;
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public IGDBScraper(ScraperConfig config, ConfigEncryptionService encryptionService) {
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
        String gameName = extractGameName(scrapInfo);
        if (gameName == null || gameName.isEmpty()) {
            gameName = gameDto.getName();
        }
        if (gameName == null) return gameDto;

        try {
            String accessToken = encryptionService.decrypt(
                    config.getEncryptedApiKey() != null ? config.getEncryptedApiKey() : "");
            String clientId = config.getHeaders() != null ? config.getHeaders().get("Client-ID") : "";

            String query = String.format("search \"%s\"; fields name,summary,cover; limit 1;", gameName);
            Request request = new Request.Builder()
                    .url(config.getApiUrl())
                    .header("Client-ID", clientId)
                    .header("Authorization", config.getAuthScheme() + " " + accessToken)
                    .post(RequestBody.create(MediaType.parse("text/plain"), query))
                    .build();

            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            JsonNode jsonNode = mapper.readTree(responseData);

            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode game = jsonNode.get(0);
                if (scrapInfo.isTitleAttr() && game.has("name")) {
                    gameDto.setName(game.get("name").asText());
                }
                if (scrapInfo.isDescriptionAttr() && game.has("summary")) {
                    gameDto.setDescription(game.get("summary").asText());
                }
                if (scrapInfo.isPosterAttr() && game.has("cover") && game.get("cover").has("url")) {
                    String coverUrl = game.get("cover").get("url").asText();
                    gameDto.setLogo("https:" + coverUrl.replace("t_thumb", "t_cover_big"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameDto;
    }

    private String extractGameName(ScrapInfo scrapInfo) {
        if (scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty()) {
            String[] parts = scrapInfo.getUrl().split("/");
            return parts[parts.length - 1].replace("-", " ").replace("_", " ");
        }
        return null;
    }
}

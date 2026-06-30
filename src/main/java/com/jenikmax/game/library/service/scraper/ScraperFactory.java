package com.jenikmax.game.library.service.scraper;

import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import com.jenikmax.game.library.service.scraper.scrapers.*;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScraperFactory {

    private final ScraperConfigService configService;
    private final ConfigEncryptionService encryptionService;
    private final OkHttpClient okHttpClient;
    private final JsoupHelper jsoupHelper;
    private final Map<String, Scraper> instanceCache = new ConcurrentHashMap<>();

    public ScraperFactory(ScraperConfigService configService, ConfigEncryptionService encryptionService,
                          OkHttpClient okHttpClient, JsoupHelper jsoupHelper) {
        this.configService = configService;
        this.encryptionService = encryptionService;
        this.okHttpClient = okHttpClient;
        this.jsoupHelper = jsoupHelper;
    }

    public Scraper getScraper(String type) {
        if (!configService.isEnabled(type)) {
            throw new IllegalArgumentException("Scraper '" + type + "' is disabled or not configured");
        }
        return instanceCache.computeIfAbsent(type, this::createScraper);
    }

    public synchronized void invalidateCache() {
        instanceCache.clear();
    }

    private Scraper createScraper(String type) {
        ScraperConfig config = configService.getConfig(type);
        if (config == null) {
            throw new IllegalArgumentException("Unknown scraper type: " + type);
        }
        switch (type) {
            case "playground":
                return new PlaygroundScraper(config, encryptionService, okHttpClient, jsoupHelper);
            case "igromania":
                return new IgromaniaScraper(config, encryptionService, jsoupHelper);
            case "steam":
                return new SteamScraper(config, encryptionService, okHttpClient);
            case "igdb":
                return new IGDBScraper(config, encryptionService, okHttpClient);
            case "thegamesdb":
                return new TheGameDBScraper(config, encryptionService, okHttpClient);
            case "worldart":
                return new WorldArtScraper(config, okHttpClient, jsoupHelper);
            case "psxdatacenter":
                return new PsxDataCenterScraper(config, okHttpClient, jsoupHelper);
            default:
                throw new IllegalArgumentException("Unsupported scraper type: " + type);
        }
    }
}

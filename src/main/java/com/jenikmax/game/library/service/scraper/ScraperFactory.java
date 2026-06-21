package com.jenikmax.game.library.service.scraper;

import com.jenikmax.game.library.service.scraper.api.Scraper;
import com.jenikmax.game.library.service.scraper.model.ScraperConfig;
import com.jenikmax.game.library.service.scraper.scrapers.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScraperFactory {

    private final ScraperConfigService configService;
    private final ConfigEncryptionService encryptionService;
    private final Map<String, Scraper> instanceCache = new ConcurrentHashMap<>();

    public ScraperFactory(ScraperConfigService configService, ConfigEncryptionService encryptionService) {
        this.configService = configService;
        this.encryptionService = encryptionService;
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
                return new PlaygroundScraper(config, encryptionService);
            case "igromania":
                return new IgromaniaScraper(config, encryptionService);
            case "steam":
                return new SteamScraper(config, encryptionService);
            case "igdb":
                return new IGDBScraper(config, encryptionService);
            case "thegamesdb":
                return new TheGameDBScraper(config, encryptionService);
            case "worldart":
                return new WorldArtScraper(config);
            default:
                throw new IllegalArgumentException("Unsupported scraper type: " + type);
        }
    }
}

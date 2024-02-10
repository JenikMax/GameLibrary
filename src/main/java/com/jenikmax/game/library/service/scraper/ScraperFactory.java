package com.jenikmax.game.library.service.scraper;

import com.jenikmax.game.library.service.scraper.api.Scraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScraperFactory {

    private static final Map<String, Scraper> scraperCache = new HashMap<>();

    @Autowired
    private ScraperFactory(List<Scraper> scraperList){
        scraperList.forEach(scraper -> scraperCache.put(scraper.getType(),scraper));
    }

    public Scraper getScraper(String type) {
        Scraper scraper = scraperCache.get(type);
        if(scraper == null) throw new IllegalArgumentException("Unknown scraper type: " + type);
        return scraper;
    }
}

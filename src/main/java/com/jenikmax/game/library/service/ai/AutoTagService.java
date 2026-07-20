package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.scraper.ScraperConfigService;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoTagService {

    private final ScraperConfigService scraperConfigService;

    public AutoTagService(ScraperConfigService scraperConfigService) {
        this.scraperConfigService = scraperConfigService;
    }

    public record AutoTagResult(List<String> suggestedTags, List<String> suggestedGenres) {}

    public AutoTagResult suggest(String description) {
        if (description == null || description.isEmpty()) {
            return new AutoTagResult(List.of(), List.of());
        }

        String cleanText = Jsoup.parse(description).text();
        String lower = cleanText.toLowerCase();

        Set<String> tags = KeywordTagMapper.matchTags(lower);
        Set<String> genres = KeywordTagMapper.matchGenres(lower);

        Map<String, List<String>> scraperGenres = scraperConfigService.getWorldArtGenreMappings();
        for (var entry : scraperGenres.entrySet()) {
            if (lower.contains(entry.getKey().toLowerCase())) {
                genres.addAll(entry.getValue());
            }
        }

        genres = genres.stream()
                .filter(this::isValidGenre)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new AutoTagResult(new ArrayList<>(tags), new ArrayList<>(genres));
    }

    public AutoTagResult preview(String text) {
        return suggest(text);
    }

    private boolean isValidGenre(String code) {
        try {
            Genre.valueOf(code);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

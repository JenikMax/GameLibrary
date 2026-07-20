package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.config.AiConfig;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private final AiClient aiClient;
    private final AiConfig aiConfig;
    private final JdbcTemplate jdbc;

    public TranslationService(AiClient aiClient, AiConfig aiConfig, JdbcTemplate jdbc) {
        this.aiClient = aiClient;
        this.aiConfig = aiConfig;
        this.jdbc = jdbc;
    }

    public String translateAndCache(Long gameId) {
        if (!isAvailable()) {
            return "";
        }

        String cached = jdbc.queryForObject(
                "SELECT description_en FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String original = jdbc.queryForObject(
                "SELECT description FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (original == null || original.isEmpty()) {
            return "";
        }

        String cleanText = Jsoup.parse(original).text();
        String direction = detectDirection(cleanText);
        String translated = translateText(cleanText, direction);

        jdbc.update(
                "UPDATE library.game_data SET description_en = ? WHERE id = ?",
                translated, gameId);

        return translated;
    }

    public boolean isAvailable() {
        return aiClient.isAvailable();
    }

    public String translateText(String text, String direction) {
        if (!isAvailable()) {
            return text;
        }

        try {
            return aiClient.translate(text, direction);
        } catch (Exception e) {
            log.error("Translation failed for direction: {}", direction, e);
            return text;
        }
    }

    private String detectDirection(String text) {
        long cyrillic = text.codePoints()
                .filter(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC)
                .count();
        return (cyrillic > text.length() * 0.3) ? "ru-en" : "en-ru";
    }
}

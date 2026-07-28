package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.config.AiConfig;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Сервис перевода описаний игр между русским и английским языками.
 * Использует AI-клиент для вызова моделей Helsinki-NLP OPUS-MT.
 * Автоматически определяет направление перевода по кириллице/латинице.
 * Кэширует результат в колонке description_translated БД.
 */
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

    /**
     * Переводит описание игры и сохраняет результат в БД. Если перевод уже есть — возвращает его.
     */
    public String translateAndCache(Long gameId) {
        if (!isAvailable()) {
            return "";
        }

        String cached = jdbc.queryForObject(
                "SELECT description_translated FROM library.game_data WHERE id = ?",
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
                "UPDATE library.game_data SET description_translated = ? WHERE id = ?",
                translated, gameId);

        return translated;
    }

    /**
     * Проверяет доступность AI-сервиса.
     */
    public boolean isAvailable() {
        return aiClient.isAvailable();
    }

    /**
     * Переводит текст в указанном направлении (ru-en или en-ru).
     */
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

    /**
     * Переводит произвольный текст с автоопределением направления.
     */
    public String translateArbitraryText(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return text;
        }
        String cleanText = Jsoup.parse(text).text();
        String direction = detectDirection(cleanText);
        return translateText(cleanText, direction);
    }

    private String detectDirection(String text) {
        long cyrillicLetters = 0;
        long latinLetters = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC && Character.isLetter(c)) {
                cyrillicLetters++;
            } else if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN && Character.isLetter(c)) {
                latinLetters++;
            }
        }
        long totalLetters = cyrillicLetters + latinLetters;
        if (totalLetters == 0) return "ru-en";
        return (cyrillicLetters > latinLetters) ? "ru-en" : "en-ru";
    }
}

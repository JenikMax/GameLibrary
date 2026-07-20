package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.config.AiConfig;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TranslationService {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);

    private final OnnxModelManager modelManager;
    private final AiConfig aiConfig;
    private final JdbcTemplate jdbc;
    private final Map<String, SentencePieceTokenizer> tokenizers = new ConcurrentHashMap<>();
    private volatile boolean modelAvailable = true;

    public TranslationService(OnnxModelManager modelManager, AiConfig aiConfig, JdbcTemplate jdbc) {
        this.modelManager = modelManager;
        this.aiConfig = aiConfig;
        this.jdbc = jdbc;
    }

    public String translateAndCache(Long gameId) {
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

    public String translateText(String text, String direction) {
        if (!aiConfig.getTranslation().isEnabled() || !modelAvailable) {
            return text;
        }

        AiConfig.TranslationModel modelConfig = aiConfig.getTranslation().getModels().get(direction);
        if (modelConfig == null) {
            log.warn("No translation model configured for direction: {}", direction);
            return text;
        }

        SentencePieceTokenizer tokenizer = getTokenizer(direction, modelConfig.getVocabFile());
        int maxLength = aiConfig.getTranslation().getMaxLength();

        try {
            return modelManager.translate(tokenizer, text,
                    modelConfig.getModelFile(), modelConfig.getVocabFile(), maxLength);
        } catch (Exception e) {
            log.error("Translation failed for direction: {}", direction, e);
            modelAvailable = false;
            return text;
        }
    }

    private String detectDirection(String text) {
        long cyrillic = text.codePoints()
                .filter(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC)
                .count();
        return (cyrillic > text.length() * 0.3) ? "ru-en" : "en-ru";
    }

    private synchronized SentencePieceTokenizer getTokenizer(String direction, String vocabFile) {
        return tokenizers.computeIfAbsent(direction, key -> {
            try {
                Path vocabPath = Path.of(aiConfig.getModelsDir()).resolve(vocabFile);
                return new SentencePieceTokenizer(vocabPath.toString());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load tokenizer for " + direction, e);
            }
        });
    }
}

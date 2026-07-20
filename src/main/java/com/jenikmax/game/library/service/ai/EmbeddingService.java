package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.config.AiConfig;
import com.jenikmax.game.library.model.entity.Game;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final OnnxModelManager modelManager;
    private final AiConfig aiConfig;
    private final JdbcTemplate jdbc;
    private SentencePieceTokenizer tokenizer;
    private volatile boolean modelAvailable = true;

    public EmbeddingService(OnnxModelManager modelManager, AiConfig aiConfig, JdbcTemplate jdbc) {
        this.modelManager = modelManager;
        this.aiConfig = aiConfig;
        this.jdbc = jdbc;
    }

    private synchronized SentencePieceTokenizer getTokenizer() {
        if (tokenizer == null) {
            AiConfig.Embedding embConfig = aiConfig.getEmbedding();
            try {
                Path vocabPath = Path.of(aiConfig.getModelsDir()).resolve(embConfig.getVocabFile());
                tokenizer = new SentencePieceTokenizer(vocabPath.toString());
            } catch (IOException e) {
                throw new RuntimeException("Failed to load embedding tokenizer", e);
            }
        }
        return tokenizer;
    }

    public float[] generateAndStore(Long gameId) {
        Game game = jdbc.queryForObject(
                "SELECT id, name, description FROM library.game_data WHERE id = ?",
                (rs, rowNum) -> {
                    Game g = new Game();
                    g.setId(rs.getLong("id"));
                    g.setName(rs.getString("name"));
                    g.setDescription(rs.getString("description"));
                    return g;
                },
                gameId);

        if (game == null || game.getDescription() == null) {
            return null;
        }

        String text = buildEmbeddingText(game);
        float[] embedding = generateEmbedding(text);
        if (embedding == null) return null;

        jdbc.update("UPDATE library.game_data SET embedding = ?::vector WHERE id = ?",
                embedding, game.getId());

        return embedding;
    }

    public float[] generateEmbedding(String text) {
        if (!aiConfig.getEmbedding().isEnabled() || !modelAvailable) {
            return null;
        }
        try {
            return modelManager.generateEmbedding(getTokenizer(), text);
        } catch (Exception e) {
            log.error("Embedding generation failed", e);
            modelAvailable = false;
            return null;
        }
    }

    public List<Long> semanticSearch(String query, int limit) {
        float[] queryEmbedding = generateEmbedding(query);
        if (queryEmbedding == null) return List.of();

        return jdbc.queryForList(
                "SELECT id FROM library.game_data " +
                "WHERE embedding IS NOT NULL " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?",
                Long.class, queryEmbedding, limit);
    }

    public boolean hasEmbeddings() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM library.game_data WHERE embedding IS NOT NULL",
                Long.class);
        return count != null && count > 0;
    }

    public int getMissingEmbeddingCount() {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM library.game_data " +
                "WHERE description IS NOT NULL AND description != '' " +
                "AND embedding IS NULL",
                Long.class);
        return count != null ? count.intValue() : 0;
    }

    private String buildEmbeddingText(Game game) {
        String desc = Jsoup.parse(game.getDescription()).text();
        return game.getName() + " " + game.getName() + " " + desc;
    }
}

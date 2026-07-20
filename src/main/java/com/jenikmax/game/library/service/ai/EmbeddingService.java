package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.model.entity.Game;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final AiClient aiClient;
    private final JdbcTemplate jdbc;

    public EmbeddingService(AiClient aiClient, JdbcTemplate jdbc) {
        this.aiClient = aiClient;
        this.jdbc = jdbc;
    }

    public boolean isAvailable() {
        return aiClient.isAvailable();
    }

    public float[] generateAndStore(Long gameId) {
        if (!isAvailable()) return null;

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
        if (!isAvailable()) {
            return null;
        }
        try {
            return aiClient.embed(text);
        } catch (Exception e) {
            log.error("Embedding generation failed", e);
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

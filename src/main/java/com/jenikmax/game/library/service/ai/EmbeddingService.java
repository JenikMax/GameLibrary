package com.jenikmax.game.library.service.ai;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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

        Object[] row = jdbc.queryForObject(
                "SELECT g.id, g.name, g.description, " +
                "COALESCE((SELECT string_agg(dg.genre_code, ' ') FROM library.game_data_genre dg WHERE dg.game_id = g.id), '') AS genres " +
                "FROM library.game_data g WHERE g.id = ?",
                (rs, rowNum) -> new Object[]{
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("genres")
                },
                gameId);

        if (row == null || row[2] == null) return null;

        String name = (String) row[1];
        String description = (String) row[2];
        String genres = (String) row[3];

        String text = buildEmbeddingText(name, description, genres);
        log.info("generateAndStore: gameId={}, textLength={}, textStart='{}'",
                gameId, text.length(),
                text.length() > 100 ? text.substring(0, 100) + "..." : text);
        float[] embedding = generateEmbedding(text);
        if (embedding == null) return null;

        jdbc.update("UPDATE library.game_data SET embedding = ?::vector WHERE id = ?",
                embedding, gameId);

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
        log.info("semanticSearch: query='{}', limit={}", query, limit);
        float[] queryEmbedding = generateEmbedding(query);
        if (queryEmbedding == null) {
            log.warn("semanticSearch: embedding generation returned null for query='{}'", query);
            return List.of();
        }
        log.info("semanticSearch: embedding generated, dims={}", queryEmbedding.length);

        List<Long> results = jdbc.queryForList(
                "SELECT id FROM library.game_data " +
                "WHERE embedding IS NOT NULL " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?",
                Long.class, queryEmbedding, limit);
        log.info("semanticSearch: returned {} results for query='{}'", results.size(), query);
        return results;
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

    private String buildEmbeddingText(String name, String description, String genres) {
        String desc = Jsoup.parse(description).text();
        return name + " " + name + " " + genres + " " + desc;
    }

    public static String fixEncoding(String input) {
        if (input == null) return null;
        return new String(input.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
    }
}

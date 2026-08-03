package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.ai.EmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbc;

    public RecommendationService(EmbeddingService embeddingService, JdbcTemplate jdbc) {
        this.embeddingService = embeddingService;
        this.jdbc = jdbc;
    }

    public boolean isAvailable() {
        return embeddingService.isAvailable() && embeddingService.hasEmbeddings();
    }

    public List<Map<String, Object>> getContentBasedSimilar(Long gameId, int limit) {
        float[] gameEmbedding = jdbc.queryForObject(
                "SELECT embedding FROM library.game_data WHERE id = ? AND embedding IS NOT NULL",
                (rs, rn) -> {
                    org.postgresql.util.PGobject obj = (org.postgresql.util.PGobject) rs.getObject("embedding");
                    if (obj == null) return null;
                    return parseVector(obj.getValue());
                },
                gameId);

        if (gameEmbedding == null) {
            logger.warn("No embedding found for game {}", gameId);
            return List.of();
        }

        List<Long> gameIds = jdbc.queryForList(
                "SELECT id FROM library.game_data " +
                "WHERE embedding IS NOT NULL AND id != ? " +
                "ORDER BY embedding <=> ?::vector " +
                "LIMIT ?",
                Long.class, gameId, gameEmbedding, limit);

        return toGameShortList(gameIds);
    }

    public List<Map<String, Object>> getUserRecommendations(Long userId, int limit) {
        List<Object[]> ratedEmbeddings = jdbc.query(
                "SELECT g.embedding FROM library.game_data g " +
                "JOIN library.game_rating r ON r.game_id = g.id " +
                "WHERE r.user_id = ? AND r.rating >= 7 AND g.embedding IS NOT NULL " +
                "ORDER BY r.rating DESC LIMIT 20",
                (rs, rn) -> {
                    org.postgresql.util.PGobject obj = (org.postgresql.util.PGobject) rs.getObject("embedding");
                    if (obj == null) return null;
                    return new Object[]{parseVector(obj.getValue())};
                },
                userId);

        if (ratedEmbeddings.isEmpty()) {
            List<Object[]> favEmbeddings = jdbc.query(
                    "SELECT g.embedding FROM library.game_data g " +
                    "JOIN library.favorite_game f ON f.game_id = g.id " +
                    "WHERE f.user_id = ? AND g.embedding IS NOT NULL " +
                    "LIMIT 10",
                    (rs, rn) -> {
                        org.postgresql.util.PGobject obj = (org.postgresql.util.PGobject) rs.getObject("embedding");
                        if (obj == null) return null;
                        return new Object[]{parseVector(obj.getValue())};
                    },
                    userId);

            if (favEmbeddings.isEmpty()) {
                return getPopularGames(limit);
            }
            float[] tasteVector = averageVectors(favEmbeddings);
            return findNearestNeighbors(tasteVector, userId, limit);
        }

        float[] tasteVector = averageVectors(ratedEmbeddings);
        return findNearestNeighbors(tasteVector, userId, limit);
    }

    private float[] averageVectors(List<Object[]> vectors) {
        if (vectors.isEmpty()) return null;
        int dims = ((float[]) vectors.get(0)[0]).length;
        float[] avg = new float[dims];
        for (Object[] row : vectors) {
            float[] vec = (float[]) row[0];
            for (int i = 0; i < dims; i++) {
                avg[i] += vec[i];
            }
        }
        for (int i = 0; i < dims; i++) {
            avg[i] /= vectors.size();
        }
        return avg;
    }

    private List<Map<String, Object>> findNearestNeighbors(float[] tasteVector, Long userId, int limit) {
        List<Long> gameIds = jdbc.queryForList(
                "SELECT gd.id FROM library.game_data gd " +
                "WHERE gd.embedding IS NOT NULL " +
                "AND gd.id NOT IN (SELECT r.game_id FROM library.game_rating r WHERE r.user_id = ?) " +
                "ORDER BY gd.embedding <=> ?::vector " +
                "LIMIT ?",
                Long.class, userId, tasteVector, limit);

        return toGameShortList(gameIds);
    }

    private List<Map<String, Object>> getPopularGames(int limit) {
        List<Long> gameIds = jdbc.queryForList(
                "SELECT g.id FROM library.game_data g " +
                "LEFT JOIN library.game_rating r ON r.game_id = g.id " +
                "GROUP BY g.id ORDER BY COUNT(r.id) DESC, g.name LIMIT ?",
                Long.class, limit);

        return toGameShortList(gameIds);
    }

    private List<Map<String, Object>> toGameShortList(List<Long> gameIds) {
        if (gameIds.isEmpty()) return List.of();

        String placeholders = String.join(",", gameIds.stream().map(String::valueOf).toArray(String[]::new));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT g.id, g.name, g.platform, g.release_date, " +
                "COALESCE((SELECT string_agg(dg.genre_code, ',' order by dg.genre_code) " +
                "  FROM library.game_data_genre dg WHERE dg.game_id = g.id), '') as genres " +
                "FROM library.game_data g WHERE g.id IN (" + placeholders + ")");

        Map<Long, Map<String, Object>> resultMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            resultMap.put(((Number) row.get("id")).longValue(), row);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : gameIds) {
            Map<String, Object> game = resultMap.get(id);
            if (game != null) {
                Map<String, Object> simple = new LinkedHashMap<>();
                simple.put("id", game.get("id"));
                simple.put("name", game.get("name"));
                simple.put("platform", game.get("platform"));
                simple.put("releaseDate", game.get("release_date"));
                simple.put("genres", game.get("genres"));
                result.add(simple);
            }
        }
        return result;
    }

    private float[] parseVector(String pgVectorString) {
        String stripped = pgVectorString.replaceAll("[\\[\\]]", "");
        String[] parts = stripped.split(",");
        float[] vec = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            vec[i] = Float.parseFloat(parts[i].trim());
        }
        return vec;
    }
}

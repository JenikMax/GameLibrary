package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.StatisticsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    private final JdbcTemplate jdbcTemplate;
    private final GameRepository gameRepository;

    public StatisticsController(JdbcTemplate jdbcTemplate, GameRepository gameRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.gameRepository = gameRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse stats = new StatisticsResponse();

        stats.setTotalGames(count("SELECT COUNT(*) FROM library.game_data"));

        stats.setRecentAdditions(count("SELECT COUNT(*) FROM library.game_data WHERE create_ts >= NOW() - INTERVAL '7 days'"));

        Double avgRating = jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(rating), 0) FROM library.game_rating", Double.class);
        stats.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0);

        stats.setTotalSizeBytes(calculateTotalSize());

        stats.setGamesByPlatform(jdbcTemplate.query(
                "SELECT platform as label, COUNT(*) as count FROM library.game_data WHERE platform IS NOT NULL AND platform != '' GROUP BY platform ORDER BY count DESC",
                (rs, rn) -> new StatisticsResponse.CountItem(rs.getString("label"), rs.getLong("count"))));

        stats.setGamesByGenre(jdbcTemplate.query(
                "SELECT gg.code, COALESCE(gg.description_ru, gg.description) as name, COUNT(gdg.game_id) as count " +
                "FROM library.game_genre gg " +
                "LEFT JOIN library.game_data_genre gdg ON gg.code = gdg.genre_code " +
                "GROUP BY gg.code, gg.description_ru, gg.description ORDER BY count DESC",
                (rs, rn) -> new StatisticsResponse.GenreCountItem(
                        rs.getString("code"), rs.getString("name"), rs.getLong("count"))));

        stats.setGamesByYear(jdbcTemplate.query(
                "SELECT SUBSTRING(release_date FROM 1 FOR 4) as label, COUNT(*) as count " +
                "FROM library.game_data WHERE release_date IS NOT NULL AND release_date != '' " +
                "GROUP BY label ORDER BY label DESC",
                (rs, rn) -> new StatisticsResponse.CountItem(rs.getString("label"), rs.getLong("count"))));

        stats.setTopRated(jdbcTemplate.query(
                "SELECT g.id, g.name, ROUND(AVG(gr.rating)::numeric, 1) as avg_rating, COUNT(gr.id) as rating_count " +
                "FROM library.game_data g JOIN library.game_rating gr ON g.id = gr.game_id " +
                "GROUP BY g.id, g.name ORDER BY avg_rating DESC LIMIT 10",
                (rs, rn) -> new StatisticsResponse.GameRatingItem(
                        rs.getLong("id"), rs.getString("name"),
                        rs.getDouble("avg_rating"), rs.getLong("rating_count"))));

        stats.setMostRated(jdbcTemplate.query(
                "SELECT g.id, g.name, ROUND(AVG(gr.rating)::numeric, 1) as avg_rating, COUNT(gr.id) as rating_count " +
                "FROM library.game_data g JOIN library.game_rating gr ON g.id = gr.game_id " +
                "GROUP BY g.id, g.name ORDER BY rating_count DESC LIMIT 10",
                (rs, rn) -> new StatisticsResponse.GameRatingItem(
                        rs.getLong("id"), rs.getString("name"),
                        rs.getDouble("avg_rating"), rs.getLong("rating_count"))));

        stats.setMostFavorited(jdbcTemplate.query(
                "SELECT g.id, g.name, COUNT(fg.id) as favorite_count " +
                "FROM library.game_data g JOIN library.favorite_game fg ON g.id = fg.game_id " +
                "GROUP BY g.id, g.name ORDER BY favorite_count DESC LIMIT 10",
                (rs, rn) -> new StatisticsResponse.GameFavItem(
                        rs.getLong("id"), rs.getString("name"), rs.getLong("favorite_count"))));

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    private long count(String sql) {
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0;
    }

    private long calculateTotalSize() {
        List<String> dirs = gameRepository.findAll().stream()
                .map(g -> g.getDirectoryPath())
                .filter(d -> d != null && !d.isBlank())
                .distinct()
                .collect(Collectors.toList());

        long total = 0;
        for (String dir : dirs) {
            Path path = Paths.get(dir);
            if (Files.exists(path)) {
                try {
                    total += Files.walk(path)
                            .filter(Files::isRegularFile)
                            .mapToLong(p -> {
                                try { return Files.size(p); } catch (IOException e) { return 0; }
                            })
                            .sum();
                } catch (IOException e) {
                    logger.warn("Failed to calculate size for {}", dir);
                }
            }
        }
        return total;
    }
}

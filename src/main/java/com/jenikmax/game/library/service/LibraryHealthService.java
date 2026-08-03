package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.dto.api.LibraryHealthReport;
import com.jenikmax.game.library.service.ai.AutoTagService;
import com.jenikmax.game.library.service.ai.EmbeddingService;
import com.jenikmax.game.library.service.ai.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibraryHealthService {

    private static final Logger logger = LoggerFactory.getLogger(LibraryHealthService.class);

    private final JdbcTemplate jdbc;
    private final EmbeddingService embeddingService;
    private final TranslationService translationService;
    private final AutoTagService autoTagService;

    public LibraryHealthService(JdbcTemplate jdbc,
                                 EmbeddingService embeddingService,
                                 TranslationService translationService,
                                 AutoTagService autoTagService) {
        this.jdbc = jdbc;
        this.embeddingService = embeddingService;
        this.translationService = translationService;
        this.autoTagService = autoTagService;
    }

    public LibraryHealthReport getHealthReport() {
        LibraryHealthReport report = new LibraryHealthReport();

        int totalGames = count("SELECT COUNT(*) FROM library.game_data");
        report.setTotalGames(totalGames);

        Map<String, LibraryHealthReport.IssueCount> counts = new LinkedHashMap<>();
        counts.put("NO_GENRES", new LibraryHealthReport.IssueCount(
                "NO_GENRES", "No genres",
                count("SELECT COUNT(DISTINCT gd.id) FROM library.game_data gd " +
                      "WHERE NOT EXISTS (SELECT 1 FROM library.game_data_genre dg WHERE dg.game_id = gd.id)"),
                true));
        counts.put("NO_DESCRIPTION", new LibraryHealthReport.IssueCount(
                "NO_DESCRIPTION", "No description",
                count("SELECT COUNT(*) FROM library.game_data WHERE description IS NULL OR LENGTH(COALESCE(description, '')) < 50"),
                false));
        counts.put("NO_TAGS", new LibraryHealthReport.IssueCount(
                "NO_TAGS", "No tags",
                count("SELECT COUNT(DISTINCT gd.id) FROM library.game_data gd " +
                      "WHERE NOT EXISTS (SELECT 1 FROM library.game_data_tag dt WHERE dt.game_id = gd.id)"),
                true));
        counts.put("NO_SCREENSHOTS", new LibraryHealthReport.IssueCount(
                "NO_SCREENSHOTS", "No screenshots",
                count("SELECT COUNT(DISTINCT gd.id) FROM library.game_data gd " +
                      "WHERE NOT EXISTS (SELECT 1 FROM library.game_screenshot gs WHERE gs.game_id = gd.id)"),
                false));
        counts.put("NO_YEAR", new LibraryHealthReport.IssueCount(
                "NO_YEAR", "No release year",
                count("SELECT COUNT(*) FROM library.game_data WHERE release_date IS NULL OR release_date = ''"),
                false));
        counts.put("NO_EMBEDDING", new LibraryHealthReport.IssueCount(
                "NO_EMBEDDING", "No embedding",
                count("SELECT COUNT(*) FROM library.game_data WHERE embedding IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50"),
                true));
        counts.put("NO_TRANSLATION", new LibraryHealthReport.IssueCount(
                "NO_TRANSLATION", "No translation",
                count("SELECT COUNT(*) FROM library.game_data WHERE description_translated IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50"),
                true));
        counts.put("PLACEHOLDER_DESC", new LibraryHealthReport.IssueCount(
                "PLACEHOLDER_DESC", "Placeholder description",
                count("SELECT COUNT(*) FROM library.game_data WHERE LOWER(COALESCE(description, '')) IN ('...', 'no description', '[no description]', 'описание отсутствует', 'n/a')"),
                false));
        report.setIssueCounts(counts);

        int filledFields = 0;
        int totalFields = totalGames * 7;
        for (Map.Entry<String, LibraryHealthReport.IssueCount> e : counts.entrySet()) {
            filledFields += totalGames - e.getValue().getCount();
        }
        report.setHealthScore(totalGames > 0 ? Math.round((double) filledFields / totalFields * 1000.0) / 10.0 : 100.0);

        report.setTopIssues(collectTopIssues(totalGames));

        return report;
    }

    public List<LibraryHealthReport.GameIssue> getIssuesByType(String issueType, int offset, int limit) {
        String sql = switch (issueType) {
            case "NO_GENRES" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE NOT EXISTS (SELECT 1 FROM library.game_data_genre dg WHERE dg.game_id = gd.id) " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_DESCRIPTION" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE description IS NULL OR LENGTH(COALESCE(description, '')) < 50 " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_TAGS" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE NOT EXISTS (SELECT 1 FROM library.game_data_tag dt WHERE dt.game_id = gd.id) " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_SCREENSHOTS" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE NOT EXISTS (SELECT 1 FROM library.game_screenshot gs WHERE gs.game_id = gd.id) " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_YEAR" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE release_date IS NULL OR release_date = '' ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_EMBEDDING" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE embedding IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50 " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "NO_TRANSLATION" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE description_translated IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50 " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            case "PLACEHOLDER_DESC" ->
                "SELECT gd.id, gd.name, COALESCE(gd.platform, '') as platform FROM library.game_data gd " +
                "WHERE LOWER(COALESCE(description, '')) IN ('...', 'no description', '[no description]', 'описание отсутствует', 'n/a') " +
                "ORDER BY gd.name LIMIT ? OFFSET ?";
            default -> null;
        };

        if (sql == null) {
            return List.of();
        }

        return jdbc.query(sql,
                (rs, rn) -> new LibraryHealthReport.GameIssue(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("platform"),
                        issueType,
                        null,
                        isTypeFixable(issueType)),
                limit, offset);
    }

    public int fixIssueType(String issueType) {
        logger.info("Starting auto-fix for issue type: {}", issueType);
        int fixed = 0;

        try {
            switch (issueType) {
                case "NO_GENRES", "NO_TAGS": {
                    List<Long> gameIds = jdbc.queryForList(
                            "SELECT gd.id FROM library.game_data gd WHERE description IS NOT NULL AND LENGTH(COALESCE(description, '')) >= 50 " +
                            (issueType.equals("NO_GENRES")
                                ? "AND NOT EXISTS (SELECT 1 FROM library.game_data_genre dg WHERE dg.game_id = gd.id)"
                                : "AND NOT EXISTS (SELECT 1 FROM library.game_data_tag dt WHERE dt.game_id = gd.id)"),
                            Long.class);
                    for (Long gameId : gameIds) {
                        try {
                            String description = jdbc.queryForObject(
                                    "SELECT description FROM library.game_data WHERE id = ?", String.class, gameId);
                            if (description != null && !description.isBlank()) {
                                var result = autoTagService.suggest(description);
                                if (!result.suggestedTags().isEmpty() || !result.suggestedGenres().isEmpty()) {
                                    if (issueType.equals("NO_TAGS")) {
                                        for (String tag : result.suggestedTags()) {
                                            jdbc.update("INSERT INTO library.game_tag (code, description, description_ru) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING",
                                                    tag, tag, tag);
                                            jdbc.update("INSERT INTO library.game_data_tag (game_id, tag_code) VALUES (?, ?) ON CONFLICT (game_id, tag_code) DO NOTHING",
                                                    gameId, tag);
                                        }
                                    }
                                    if (issueType.equals("NO_GENRES")) {
                                        for (String genre : result.suggestedGenres()) {
                                            jdbc.update("INSERT INTO library.game_data_genre (game_id, genre_code) VALUES (?, ?) ON CONFLICT (game_id, genre_code) DO NOTHING",
                                                    gameId, genre);
                                        }
                                    }
                                    fixed++;
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to auto-tag game {}: {}", gameId, e.getMessage());
                        }
                    }
                    break;
                }
                case "NO_EMBEDDING": {
                    List<Long> gameIds = jdbc.queryForList(
                            "SELECT id FROM library.game_data WHERE embedding IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50",
                            Long.class);
                    for (Long gameId : gameIds) {
                        try {
                            embeddingService.generateAndStore(gameId);
                            fixed++;
                        } catch (Exception e) {
                            logger.warn("Failed to generate embedding for game {}: {}", gameId, e.getMessage());
                        }
                    }
                    break;
                }
                case "NO_TRANSLATION": {
                    List<Long> gameIds = jdbc.queryForList(
                            "SELECT id FROM library.game_data WHERE description_translated IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50",
                            Long.class);
                    for (Long gameId : gameIds) {
                        try {
                            translationService.translateAndCache(gameId);
                            fixed++;
                        } catch (Exception e) {
                            logger.warn("Failed to translate game {}: {}", gameId, e.getMessage());
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Auto-fix failed for issue type {}: {}", issueType, e.getMessage(), e);
        }

        logger.info("Auto-fix for {} complete: {} games fixed", issueType, fixed);
        return fixed;
    }

    public int fixSingleGame(Long gameId, String issueType) {
        try {
            switch (issueType) {
                case "NO_GENRES": {
                    String description = jdbc.queryForObject(
                            "SELECT description FROM library.game_data WHERE id = ?", String.class, gameId);
                    if (description != null && !description.isBlank()) {
                        var result = autoTagService.suggest(description);
                        for (String genre : result.suggestedGenres()) {
                            jdbc.update("INSERT INTO library.game_data_genre (game_id, genre_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                    gameId, genre);
                        }
                    }
                    break;
                }
                case "NO_TAGS": {
                    String description = jdbc.queryForObject(
                            "SELECT description FROM library.game_data WHERE id = ?", String.class, gameId);
                    if (description != null && !description.isBlank()) {
                        var result = autoTagService.suggest(description);
                        for (String tag : result.suggestedTags()) {
                            jdbc.update("INSERT INTO library.game_tag (code, description, description_ru) VALUES (?, ?, ?) ON CONFLICT (code) DO NOTHING",
                                    tag, tag, tag);
                            jdbc.update("INSERT INTO library.game_data_tag (game_id, tag_code) VALUES (?, ?) ON CONFLICT DO NOTHING",
                                    gameId, tag);
                        }
                    }
                    break;
                }
                case "NO_EMBEDDING": {
                    embeddingService.generateAndStore(gameId);
                    break;
                }
                case "NO_TRANSLATION": {
                    translationService.translateAndCache(gameId);
                    break;
                }
            }
            return 1;
        } catch (Exception e) {
            logger.warn("Failed to fix single game {} for {}: {}", gameId, issueType, e.getMessage());
            return 0;
        }
    }

    private boolean isTypeFixable(String issueType) {
        return switch (issueType) {
            case "NO_GENRES", "NO_TAGS", "NO_EMBEDDING", "NO_TRANSLATION" -> true;
            default -> false;
        };
    }

    private List<LibraryHealthReport.GameIssue> collectTopIssues(int totalGames) {
        List<LibraryHealthReport.GameIssue> issues = new ArrayList<>();
        issues.addAll(getIssuesByType("NO_DESCRIPTION", 0, 3));
        issues.addAll(getIssuesByType("NO_GENRES", 0, 3));
        issues.addAll(getIssuesByType("NO_SCREENSHOTS", 0, 3));
        if (issues.size() > 10) {
            return issues.subList(0, 10);
        }
        return issues;
    }

    private int count(String sql) {
        Long result = jdbc.queryForObject(sql, Long.class);
        return result != null ? result.intValue() : 0;
    }
}

package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.dto.api.LibraryHealthReport;
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

    public LibraryHealthService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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
                false));
        counts.put("NO_DESCRIPTION", new LibraryHealthReport.IssueCount(
                "NO_DESCRIPTION", "No description",
                count("SELECT COUNT(*) FROM library.game_data WHERE description IS NULL OR description = '' OR description = 'N/A'"),
                false));
        counts.put("NO_TAGS", new LibraryHealthReport.IssueCount(
                "NO_TAGS", "No tags",
                count("SELECT COUNT(DISTINCT gd.id) FROM library.game_data gd " +
                      "WHERE NOT EXISTS (SELECT 1 FROM library.game_data_tag dt WHERE dt.game_id = gd.id)"),
                false));
        counts.put("NO_SCREENSHOTS", new LibraryHealthReport.IssueCount(
                "NO_SCREENSHOTS", "No screenshots",
                count("SELECT COUNT(DISTINCT gd.id) FROM library.game_data gd " +
                      "WHERE NOT EXISTS (SELECT 1 FROM library.game_screenshot gs WHERE gs.game_id = gd.id)"),
                false));
        counts.put("NO_YEAR", new LibraryHealthReport.IssueCount(
                "NO_YEAR", "No release year",
                count("SELECT COUNT(*) FROM library.game_data WHERE release_date IS NULL OR release_date = '' OR release_date = 'N/A'"),
                false));
        counts.put("NO_EMBEDDING", new LibraryHealthReport.IssueCount(
                "NO_EMBEDDING", "No embedding",
                count("SELECT COUNT(*) FROM library.game_data WHERE embedding IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50"),
                false));
        counts.put("NO_TRANSLATION", new LibraryHealthReport.IssueCount(
                "NO_TRANSLATION", "No translation",
                count("SELECT COUNT(*) FROM library.game_data WHERE description_translated IS NULL AND description IS NOT NULL AND LENGTH(description) >= 50"),
                false));
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
                "WHERE description IS NULL OR description = '' OR description = 'N/A' " +
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
                "WHERE release_date IS NULL OR release_date = '' OR release_date = 'N/A' ORDER BY gd.name LIMIT ? OFFSET ?";
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
                        false),
                limit, offset);
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

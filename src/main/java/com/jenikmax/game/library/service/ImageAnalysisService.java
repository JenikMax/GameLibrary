package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    private final AiClient aiClient;
    private final JdbcTemplate jdbc;
    private final String imagesDirectory;

    public ImageAnalysisService(AiClient aiClient,
                                 JdbcTemplate jdbc,
                                 @Value("${game-library.images.directory:/gameLibrary/images}") String imagesDirectory) {
        this.aiClient = aiClient;
        this.jdbc = jdbc;
        this.imagesDirectory = imagesDirectory;
    }

    public boolean isAvailable() {
        return aiClient.isVisionAvailable();
    }

    public Map<String, Object> analyzeGameScreenshots(Long gameId, int maxScreenshots) {
        List<byte[]> screenshots = loadScreenshotData(gameId, maxScreenshots);
        if (screenshots.isEmpty()) {
            return Map.of("suggestedTags", List.of(), "suggestedGenres", List.of(),
                    "message", "No screenshots found for this game");
        }

        List<String> allLabels = buildLabelList();

        List<AiClient.LabelMatch> matches;
        if (screenshots.size() == 1) {
            matches = aiClient.classifyImage(screenshots.get(0), allLabels, 10);
        } else {
            matches = aiClient.classifyImagesMulti(screenshots, allLabels, 10);
        }

        if (matches.isEmpty()) {
            return Map.of("suggestedTags", List.of(), "suggestedGenres", List.of(),
                    "message", "Vision model returned no matches");
        }

        List<String> suggestedTags = new ArrayList<>();
        List<String> suggestedGenres = new ArrayList<>();

        List<String> tagCodes = getTagCodes();
        List<String> genreCodes = getGenreCodes();

        for (AiClient.LabelMatch match : matches) {
            if (genreCodes.contains(match.label())) {
                suggestedGenres.add(match.label());
            }
            if (tagCodes.contains(match.label())) {
                suggestedTags.add(match.label());
            }
        }

        return Map.of("suggestedTags", suggestedTags, "suggestedGenres", suggestedGenres,
                "matches", matches.stream()
                        .map(m -> Map.of("label", m.label(), "score", m.score()))
                        .collect(Collectors.toList()));
    }

    private List<String> buildLabelList() {
        List<String> labels = new ArrayList<>();
        for (Genre genre : Genre.values()) {
            labels.add(genre.name());
        }
        List<String> tagCodes = jdbc.queryForList(
                "SELECT code FROM library.game_tag", String.class);
        labels.addAll(tagCodes);
        return labels;
    }

    private List<String> getTagCodes() {
        return jdbc.queryForList("SELECT code FROM library.game_tag", String.class);
    }

    private List<String> getGenreCodes() {
        return Arrays.stream(Genre.values()).map(Genre::name).collect(Collectors.toList());
    }

    private List<byte[]> loadScreenshotData(Long gameId, int maxCount) {
        List<byte[]> data = new ArrayList<>();
        Path gameDir = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "screenshots");
        if (Files.isDirectory(gameDir)) {
            try (var stream = Files.list(gameDir)) {
                stream.filter(Files::isRegularFile)
                        .limit(maxCount)
                        .forEach(p -> {
                            try {
                                data.add(Files.readAllBytes(p));
                            } catch (IOException e) {
                                logger.warn("Failed to read screenshot: {}", p, e);
                            }
                        });
            } catch (IOException e) {
                logger.warn("Failed to list screenshot dir: {}", gameDir, e);
            }
        }
        if (data.isEmpty()) {
            List<byte[]> dbImages = jdbc.queryForList(
                    "SELECT source FROM library.game_screenshot WHERE game_id = ? LIMIT ?",
                    byte[].class, gameId, maxCount);
            data.addAll(dbImages);
        }
        return data;
    }
}

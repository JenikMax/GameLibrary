package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.ai.AiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.springframework.context.MessageSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImageAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(ImageAnalysisService.class);

    private final AiClient aiClient;
    private final JdbcTemplate jdbc;
    private final MessageSource messageSource;
    private final String imagesDirectory;

    public ImageAnalysisService(AiClient aiClient,
                                 JdbcTemplate jdbc,
                                 MessageSource messageSource,
                                 @Value("${game-library.images.directory:/gameLibrary/images}") String imagesDirectory) {
        this.aiClient = aiClient;
        this.jdbc = jdbc;
        this.messageSource = messageSource;
        this.imagesDirectory = imagesDirectory;
    }

    public boolean isAvailable() {
        return aiClient.isVisionAvailable();
    }

    public Map<String, Object> analyzeGameScreenshots(Long gameId, int maxScreenshots, Locale locale) {
        List<byte[]> screenshots = loadScreenshotData(gameId, maxScreenshots);
        if (screenshots.isEmpty()) {
            logger.info("Screenshot analysis for game {}: no screenshots found", gameId);
            return Map.of("suggestedTags", List.of(), "suggestedGenres", List.of(),
                    "message", "No screenshots found for this game");
        }

        logger.info("Screenshot analysis for game {}: loaded {} screenshots ({} bytes total), labels: {}",
                gameId, screenshots.size(),
                screenshots.stream().mapToLong(b -> b.length).sum(),
                buildLabelList().size());

        List<String> allLabels = buildLabelList();
        logger.debug("Label list for game {}: {} genres + {} tags = {} total labels",
                gameId, Genre.values().length, getTagCodes().size(), allLabels.size());

        List<AiClient.LabelMatch> matches;
        if (screenshots.size() == 1) {
            logger.info("Screenshot analysis for game {}: using single-image classify", gameId);
            matches = aiClient.classifyImage(screenshots.get(0), allLabels, 10);
        } else {
            logger.info("Screenshot analysis for game {}: using multi-image classify ({} images)", gameId, screenshots.size());
            matches = aiClient.classifyImagesMulti(screenshots, allLabels, 10);
        }

        if (matches.isEmpty()) {
            logger.warn("Screenshot analysis for game {}: CLIP returned no matches", gameId);
            return Map.of("suggestedTags", List.of(), "suggestedGenres", List.of(),
                    "message", "Vision model returned no matches");
        }

        logger.info("Screenshot analysis for game {}: CLIP returned {} matches: {}",
                gameId, matches.size(),
                matches.stream().map(m -> m.label() + "=" + m.score()).collect(Collectors.joining(", ")));

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

        Map<String, String> tagNames = getTagLocalizedNames(locale);
        Map<String, String> genreNames = new LinkedHashMap<>();
        for (String code : suggestedGenres) {
            genreNames.put(code, messageSource.getMessage("enum.genre." + code, null, code, locale));
        }

        return Map.of("suggestedTags", suggestedTags, "suggestedGenres", suggestedGenres,
                "tagNames", suggestedTags.stream().collect(Collectors.toMap(t -> t, t -> tagNames.getOrDefault(t, t))),
                "genreNames", genreNames,
                "matches", matches.stream()
                        .map(m -> Map.of("label", m.label(), "score", m.score()))
                        .collect(Collectors.toList()));
    }

    private Map<String, String> getTagLocalizedNames(Locale locale) {
        String col = "ru".equals(locale.getLanguage()) ? "description_ru" : "description";
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT code, " + col + " as name FROM library.game_tag");
        Map<String, String> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String code = (String) row.get("code");
            String name = (String) row.get("name");
            if (code != null) {
                result.put(code, name != null ? name : code);
            }
        }
        return result;
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
                logger.info("Screenshot analysis for game {}: loaded {} screenshots from filesystem (dir: {})",
                        gameId, data.size(), gameDir);
            } catch (IOException e) {
                logger.warn("Failed to list screenshot dir: {}", gameDir, e);
            }
        }
        if (data.isEmpty()) {
            List<byte[]> dbImages = jdbc.queryForList(
                    "SELECT source FROM library.game_screenshot WHERE game_id = ? LIMIT ?",
                    byte[].class, gameId, maxCount);
            data.addAll(dbImages);
            logger.info("Screenshot analysis for game {}: loaded {} screenshots from database", gameId, data.size());
        }
        return data;
    }
}

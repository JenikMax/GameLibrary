package com.jenikmax.game.library.service.utils;

import com.jenikmax.game.library.dao.api.GameRepository;
import com.jenikmax.game.library.dao.api.ScreenshotRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ImageMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(ImageMigrationService.class);

    private static final int BATCH_SIZE = 50;

    private final String imagesDirectory;
    private final GameRepository gameRepository;
    private final ScreenshotRepository screenshotRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ImageMigrationService(
            @Value("${game-library.images.directory:/gameLibrary/images}") String imagesDirectory,
            GameRepository gameRepository,
            ScreenshotRepository screenshotRepository,
            UserRepository userRepository) {
        this.imagesDirectory = imagesDirectory;
        this.gameRepository = gameRepository;
        this.screenshotRepository = screenshotRepository;
        this.userRepository = userRepository;
    }

    public int migrateAll() {
        int count = 0;
        count += migrateGameLogos();
        count += migrateScreenshots();
        count += migrateAvatars();
        return count;
    }

    @Transactional(readOnly = true)
    public int migrateGameLogos() {
        int count = 0;
        int page = 0;
        Page<Game> gamesPage;
        while ((gamesPage = gameRepository.findAll(PageRequest.of(page++, BATCH_SIZE))).hasContent()) {
            for (Game game : gamesPage.getContent()) {
                if (game.getLogo() != null) {
                    try {
                        Path dir = Paths.get(imagesDirectory, "games", String.valueOf(game.getId()));
                        Files.createDirectories(dir);
                        Path file = dir.resolve("logo.jpg");
                        if (!Files.exists(file)) {
                            Files.write(file, game.getLogo());
                            count++;
                        }
                    } catch (IOException e) {
                        logger.error("Failed to migrate logo for game {}", game.getId(), e);
                    }
                }
            }
            entityManager.clear();
        }
        logger.info("Migrated {} game logos", count);
        return count;
    }

    @Transactional(readOnly = true)
    public int migrateScreenshots() {
        int count = 0;
        int page = 0;
        Page<Screenshot> screenshotsPage;
        while ((screenshotsPage = screenshotRepository.findAll(PageRequest.of(page++, BATCH_SIZE))).hasContent()) {
            for (Screenshot ss : screenshotsPage.getContent()) {
                if (ss.getSource() != null) {
                    try {
                        Path dir = Paths.get(imagesDirectory, "games",
                                String.valueOf(ss.getGame().getId()), "screenshots");
                        Files.createDirectories(dir);
                        Path file = dir.resolve(ss.getId() + ".jpg");
                        if (!Files.exists(file)) {
                            Files.write(file, ss.getSource());
                            count++;
                        }
                    } catch (IOException e) {
                        logger.error("Failed to migrate screenshot {}", ss.getId(), e);
                    }
                }
            }
            entityManager.clear();
        }
        logger.info("Migrated {} screenshots", count);
        return count;
    }

    @Transactional(readOnly = true)
    public int migrateAvatars() {
        int count = 0;
        int page = 0;
        Page<User> usersPage;
        while ((usersPage = userRepository.findAll(PageRequest.of(page++, BATCH_SIZE))).hasContent()) {
            for (User user : usersPage.getContent()) {
                if (user.getAvatar() != null) {
                    try {
                        Path dir = Paths.get(imagesDirectory, "avatars");
                        Files.createDirectories(dir);
                        Path file = dir.resolve(user.getId() + ".jpg");
                        if (!Files.exists(file)) {
                            Files.write(file, user.getAvatar());
                            count++;
                        }
                    } catch (IOException e) {
                        logger.error("Failed to migrate avatar for user {}", user.getId(), e);
                    }
                }
            }
            entityManager.clear();
        }
        logger.info("Migrated {} avatars", count);
        return count;
    }
}

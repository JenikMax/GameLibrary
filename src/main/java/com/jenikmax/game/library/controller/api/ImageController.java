package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameRepository;
import com.jenikmax.game.library.dao.api.ScreenshotRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final Logger logger = LoggerFactory.getLogger(ImageController.class);

    private final String imagesDirectory;
    private final GameRepository gameRepository;
    private final ScreenshotRepository screenshotRepository;
    private final UserRepository userRepository;

    public ImageController(
            @Value("${game-library.images.directory:/gameLibrary/images}") String imagesDirectory,
            GameRepository gameRepository,
            ScreenshotRepository screenshotRepository,
            UserRepository userRepository) {
        this.imagesDirectory = imagesDirectory;
        this.gameRepository = gameRepository;
        this.screenshotRepository = screenshotRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/games/{gameId}/logo")
    public ResponseEntity<byte[]> getGameLogo(@PathVariable Long gameId) {
        Path filePath = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "logo.jpg");
        if (Files.exists(filePath)) {
            return serveFile(filePath);
        }
        Game game = gameRepository.getReferenceById(gameId);
        if (game != null && game.getLogo() != null) {
                    return ResponseEntity.ok()
                            .header("Cache-Control", "no-cache, no-store, must-revalidate")
                            .contentType(MediaType.IMAGE_JPEG)
                            .body(game.getLogo());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/games/{gameId}/screenshots/{screenshotId}")
    public ResponseEntity<byte[]> getScreenshot(@PathVariable Long gameId, @PathVariable Long screenshotId) {
        Path filePath = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "screenshots", screenshotId + ".jpg");
        if (Files.exists(filePath)) {
            return serveFile(filePath);
        }
        Optional<Screenshot> screenshot = screenshotRepository.findById(screenshotId);
        if (screenshot.isPresent() && screenshot.get().getSource() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(screenshot.get().getSource());
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/avatars/{userId}")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        Path filePath = Paths.get(imagesDirectory, "avatars", userId + ".jpg");
        if (Files.exists(filePath)) {
            return serveFile(filePath);
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent() && user.get().getAvatar() != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(user.get().getAvatar());
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<byte[]> serveFile(Path filePath) {
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(bytes);
        } catch (IOException e) {
            logger.error("Error serving file: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

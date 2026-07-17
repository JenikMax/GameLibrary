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
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
    private static final long CACHE_MAX_AGE = 86400;

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
    public ResponseEntity<Resource> getGameLogo(
            @PathVariable Long gameId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) throws IOException {
        Path filePath = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "logo.jpg");
        if (Files.exists(filePath)) {
            return serveFileStream(filePath, generateFileETag(filePath), ifNoneMatch);
        }
        Optional<Game> gameOpt = gameRepository.findById(gameId);
        if (gameOpt.isPresent() && gameOpt.get().getLogo() != null) {
            byte[] logo = gameOpt.get().getLogo();
            String etag = generateBytesETag(logo);
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE)
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new InputStreamResource(new java.io.ByteArrayInputStream(logo)));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/games/{gameId}/screenshots/{screenshotId}")
    public ResponseEntity<Resource> getScreenshot(
            @PathVariable Long gameId,
            @PathVariable Long screenshotId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) throws IOException {
        Path filePath = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "screenshots", screenshotId + ".jpg");
        if (Files.exists(filePath)) {
            return serveFileStream(filePath, generateFileETag(filePath), ifNoneMatch);
        }
        Optional<Screenshot> screenshot = screenshotRepository.findById(screenshotId);
        if (screenshot.isPresent() && screenshot.get().getSource() != null) {
            byte[] data = screenshot.get().getSource();
            String etag = generateBytesETag(data);
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE)
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new InputStreamResource(new java.io.ByteArrayInputStream(data)));
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/avatars/{userId}")
    public ResponseEntity<Resource> getAvatar(
            @PathVariable Long userId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) throws IOException {
        Path filePath = Paths.get(imagesDirectory, "avatars", userId + ".jpg");
        if (Files.exists(filePath)) {
            return serveFileStream(filePath, generateFileETag(filePath), ifNoneMatch);
        }
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent() && user.get().getAvatar() != null) {
            byte[] avatar = user.get().getAvatar();
            String etag = generateBytesETag(avatar);
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE)
                    .header(HttpHeaders.ETAG, etag)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new InputStreamResource(new java.io.ByteArrayInputStream(avatar)));
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<Resource> serveFileStream(Path filePath, String etag, String ifNoneMatch) throws IOException {
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) contentType = "application/octet-stream";
        
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=" + CACHE_MAX_AGE)
                .header(HttpHeaders.ETAG, etag)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new FileSystemResource(filePath));
    }

    private String generateFileETag(Path filePath) throws IOException {
        long lastModified = Files.getLastModifiedTime(filePath).toMillis();
        long size = Files.size(filePath);
        return "\"" + Long.toHexString(lastModified) + "-" + Long.toHexString(size) + "\"";
    }

    private String generateBytesETag(byte[] data) {
        int hash = java.util.Arrays.hashCode(data);
        return "\"" + Integer.toHexString(hash) + "-" + Integer.toHexString(data.length) + "\"";
    }
}

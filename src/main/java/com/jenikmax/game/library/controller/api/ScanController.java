package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScanController {

    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);

    private final LibraryService libraryService;
    private final NotificationService notificationService;
    private final UserService userService;

    public ScanController(LibraryService libraryService,
                           NotificationService notificationService,
                           UserService userService) {
        this.libraryService = libraryService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Void>> scanLibrary() {
        logger.info("REST scan library");
        try {
            libraryService.scanLibrary();
            Long userId = getCurrentUserId();
            if (userId != null) {
                notificationService.create(userId, "scan_complete",
                        "Сканирование завершено",
                        "Библиотека успешно просканирована", null);
            }
            return ResponseEntity.ok(ApiResponse.ok("Library scan completed", null));
        } catch (Exception e) {
            logger.error("Scan error", e);
            Long userId = getCurrentUserId();
            if (userId != null) {
                notificationService.create(userId, "scan_failed",
                        "Ошибка сканирования",
                        "Не удалось просканировать библиотеку: " + e.getMessage(), null);
            }
            return ResponseEntity.internalServerError().body(ApiResponse.error("Scan failed: " + e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

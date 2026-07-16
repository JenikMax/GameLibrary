package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.notification.NotificationService;
import com.jenikmax.game.library.service.scaner.ScanTask;
import com.jenikmax.game.library.service.scaner.ScanTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScanController {

    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);

    private final LibraryService libraryService;
    private final ScanTaskService scanTaskService;
    private final NotificationService notificationService;
    private final UserService userService;

    public ScanController(LibraryService libraryService,
                           ScanTaskService scanTaskService,
                           NotificationService notificationService,
                           UserService userService) {
        this.libraryService = libraryService;
        this.scanTaskService = scanTaskService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @PostMapping("/scan")
    public ResponseEntity<ApiResponse<Map<String, Object>>> scanLibrary() {
        logger.info("REST scan library (async)");
        String taskId = scanTaskService.submitScanTask();

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", taskId);
        data.put("statusUrl", "/game-library/api/scan/status/" + taskId);

        return ResponseEntity.accepted()
                .body(ApiResponse.ok("Library scan started", data));
    }

    @GetMapping("/scan/status/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScanStatus(@PathVariable String taskId) {
        ScanTask task = scanTaskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        data.put("status", task.getStatus().name());
        data.put("progress", task.getProgress());
        data.put("currentGame", task.getCurrentGame());
        data.put("newGamesCount", task.getNewGamesCount());
        data.put("deletedGamesCount", task.getDeletedGamesCount());
        data.put("totalCount", task.getTotalCount());
        data.put("errorMessage", task.getErrorMessage());

        if (task.getStatus() == ScanTask.Status.COMPLETED && !task.isNotified()) {
            task.setNotified(true);
            Long userId = getCurrentUserId();
            if (userId != null) {
                notificationService.create(userId, "scan_complete",
                        "Сканирование завершено",
                        "Библиотека успешно просканирована", null);
            }
        } else if (task.getStatus() == ScanTask.Status.FAILED && !task.isNotified()) {
            task.setNotified(true);
            Long userId = getCurrentUserId();
            if (userId != null) {
                notificationService.create(userId, "scan_failed",
                        "Ошибка сканирования",
                        "Не удалось просканировать библиотеку: " + task.getErrorMessage(), null);
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.config.jwt.JwtTokenProvider;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.Notification;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.notification.NotificationService;
import com.jenikmax.game.library.service.notification.SseService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Notifications", description = "User notifications")
/**
 * Контроллер уведомлений пользователя.
 * Обрабатывает запросы по пути /api/notifications.
 * Поддерживает получение списка уведомлений, отметку прочитанными (одного
 * или всех), а также SSE-подписку для получения уведомлений в реальном времени.
 */
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public NotificationController(NotificationService notificationService, SseService sseService,
                                   UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.notificationService = notificationService;
        this.sseService = sseService;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Подписаться на SSE-уведомления. Токен передаётся как query-параметр
     * для аутентификации (т.к. SSE не поддерживает заголовки).
     * @param token JWT-токен для аутентификации
     * @return SseEmitter для получения событий в реальном времени
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("token") String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            SseEmitter error = new SseEmitter(0L);
            try {
                error.send(SseEmitter.event().name("error").data("Unauthorized"));
            } catch (java.io.IOException ignored) {}
            error.complete();
            return error;
        }
        String username = jwtTokenProvider.getUsernameFromToken(token);
        var userDto = userService.getUserInfoByName(username);
        if (userDto == null) {
            SseEmitter error = new SseEmitter(0L);
            try {
                error.send(SseEmitter.event().name("error").data("Unauthorized"));
            } catch (java.io.IOException ignored) {}
            error.complete();
            return error;
        }
        return sseService.subscribe(userDto.getId());
    }

    /**
     * Получить список последних уведомлений и количество непрочитанных.
     * @return список уведомлений и поле unread
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<Notification> list = notificationService.getRecent(userId);
        long unread = notificationService.getUnreadCount(userId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Notification n : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("gameId", n.getGameId());
            m.put("read", n.isRead());
            m.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : null);
            items.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("items", items, "unread", unread)));
    }

    /**
     * Отметить одно уведомление как прочитанное.
     * @param id идентификатор уведомления
     * @return пустой ответ
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Отметить все уведомления текущего пользователя как прочитанные.
     * @return пустой ответ
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

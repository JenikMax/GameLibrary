package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameCommentRepository;
import com.jenikmax.game.library.dao.api.UserRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameComment;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.*;

@RestController
@RequestMapping("/api/games")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Comments", description = "Game comments")
/**
 * Контроллер комментариев к играм.
 * Обрабатывает запросы по пути /api/games/{id}/comments.
 * Поддерживает получение, добавление и удаление комментариев.
 * Проверяет права владельца комментария при удалении; администратор может
 * удалить любой комментарий.
 */
public class CommentController {

    private final GameCommentRepository commentRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    public CommentController(GameCommentRepository commentRepository,
                             UserService userService,
                             UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * Получить все комментарии к игре, отсортированные по дате (сначала новые).
     * @param id идентификатор игры
     * @return список комментариев с флагом canDelete для текущего пользователя
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getComments(@PathVariable Long id) {
        List<GameComment> comments = commentRepository.findByGameIdOrderByCreatedAtDesc(id);
        Long currentUserId = getCurrentUserId();
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameComment c : comments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("text", c.getText());
            m.put("userId", c.getUser().getId());
            m.put("username", c.getUser().getUsername());
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
            m.put("canDelete", currentUserId != null && (currentUserId.equals(c.getUser().getId()) || isAdmin()));
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Добавить новый комментарий к игре.
     * @param id   идентификатор игры
     * @param body JSON с полем text
     * @return созданный комментарий с метаданными
     */
    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        String text = body.get("text");
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Comment text is required"));
        }

        GameComment comment = new GameComment();
        comment.setGame(new Game());
        comment.getGame().setId(id);
        comment.setUser(new User());
        comment.getUser().setId(userId);
        comment.setText(text.trim());
        Timestamp now = new Timestamp(new Date().getTime());
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentRepository.save(comment);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", comment.getId());
        m.put("text", comment.getText());
        m.put("userId", userId);
        m.put("username", userRepository.findById(userId).map(User::getUsername).orElse(""));
        m.put("createdAt", now.toString());
        m.put("canDelete", true);
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    /**
     * Удалить комментарий. Доступно автору комментария или администратору.
     * @param gameId    идентификатор игры
     * @param commentId идентификатор комментария
     * @return сообщение об успешном удалении или 403 при отсутствии прав
     */
    @DeleteMapping("/{gameId}/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long gameId,
            @PathVariable Long commentId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        var opt = commentRepository.findById(commentId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GameComment comment = opt.get();
        if (!userId.equals(comment.getUser().getId()) && !isAdmin()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        }
        commentRepository.delete(comment);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

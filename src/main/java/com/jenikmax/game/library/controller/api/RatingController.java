package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameRatingRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameRating;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/games")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Ratings", description = "Game ratings 1-10")
/**
 * Контроллер оценок игр (рейтинг 1-10).
 * Обрабатывает запросы по пути /api/games/{id}/rating.
 * Поддерживает получение среднего рейтинга и оценки текущего пользователя,
 * а также добавление/обновление оценки.
 */
public class RatingController {

    private final GameRatingRepository ratingRepository;
    private final UserService userService;

    public RatingController(GameRatingRepository ratingRepository, UserService userService) {
        this.ratingRepository = ratingRepository;
        this.userService = userService;
    }

    /**
     * Получить рейтинг игры: средняя оценка, количество оценок, оценка текущего пользователя.
     * @param id идентификатор игры
     * @return средний рейтинг, количество и оценка пользователя
     */
    @GetMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRating(@PathVariable Long id) {
        Double avg = ratingRepository.findAvgRatingByGameId(id);
        Long count = ratingRepository.countRatingsByGameId(id);

        Long userId = getCurrentUserId();
        Integer userRating = null;
        if (userId != null) {
            Optional<Integer> ur = ratingRepository.findUserRating(id, userId);
            if (ur.isPresent()) userRating = ur.get();
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "avgRating", avg != null ? Math.round(avg * 10.0) / 10.0 : null,
                "ratingsCount", count != null ? count : 0L,
                "userRating", userRating
        )));
    }

    /**
     * Оценить игру (1-10). При повторном вызове обновляет существующую оценку.
     * @param id   идентификатор игры
     * @param body JSON с полем rating (число от 1 до 10)
     * @return обновлённый средний рейтинг и оценка пользователя
     */
    @PostMapping("/{id}/rating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rateGame(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        Integer rating = body.get("rating");
        if (rating == null || rating < 1 || rating > 10) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Rating must be between 1 and 10"));
        }

        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        Optional<GameRating> existing = ratingRepository.findByGameIdAndUserId(id, userId);
        GameRating gr;
        if (existing.isPresent()) {
            gr = existing.get();
            gr.setRating(rating);
        } else {
            gr = new GameRating();
            gr.setGame(new Game());
            gr.getGame().setId(id);
            gr.setUser(new User());
            gr.getUser().setId(userId);
            gr.setRating(rating);
            gr.setCreatedAt(new Timestamp(new Date().getTime()));
        }
        ratingRepository.save(gr);

        Double avg = ratingRepository.findAvgRatingByGameId(id);
        Long count = ratingRepository.countRatingsByGameId(id);

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "avgRating", avg != null ? Math.round(avg * 10.0) / 10.0 : null,
                "ratingsCount", count != null ? count : 0L,
                "userRating", rating
        )));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameReviewRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameReview;
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
@io.swagger.v3.oas.annotations.tags.Tag(name = "Reviews", description = "Game reviews with category scores")
public class ReviewController {

    private final GameReviewRepository reviewRepository;
    private final UserService userService;

    public ReviewController(GameReviewRepository reviewRepository, UserService userService) {
        this.reviewRepository = reviewRepository;
        this.userService = userService;
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviews(@PathVariable Long id) {
        List<GameReview> reviews = reviewRepository.findByGameIdOrderByCreatedAtDesc(id);
        Long currentUserId = getCurrentUserId();

        Double avgGameplay = reviewRepository.findAvgGameplayScore(id);
        Double avgGraphics = reviewRepository.findAvgGraphicsScore(id);
        Double avgStory = reviewRepository.findAvgStoryScore(id);
        Double avgMusic = reviewRepository.findAvgMusicScore(id);

        Map<String, Object> aggregated = new LinkedHashMap<>();
        aggregated.put("gameplay", avgGameplay != null ? Math.round(avgGameplay * 10.0) / 10.0 : null);
        aggregated.put("graphics", avgGraphics != null ? Math.round(avgGraphics * 10.0) / 10.0 : null);
        aggregated.put("story", avgStory != null ? Math.round(avgStory * 10.0) / 10.0 : null);
        aggregated.put("music", avgMusic != null ? Math.round(avgMusic * 10.0) / 10.0 : null);

        List<Map<String, Object>> reviewList = new ArrayList<>();
        for (GameReview r : reviews) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("text", r.getText());
            m.put("pros", r.getPros());
            m.put("cons", r.getCons());
            m.put("gameplayScore", r.getGameplayScore());
            m.put("graphicsScore", r.getGraphicsScore());
            m.put("storyScore", r.getStoryScore());
            m.put("musicScore", r.getMusicScore());
            m.put("userId", r.getUser().getId());
            m.put("username", r.getUser().getUsername());
            m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            m.put("canDelete", currentUserId != null && (currentUserId.equals(r.getUser().getId()) || isAdmin()));
            reviewList.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reviews", reviewList);
        result.put("aggregatedScores", aggregated);
        result.put("count", reviews.size());

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addOrUpdateReview(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        String text = (String) body.get("text");
        String pros = (String) body.get("pros");
        String cons = (String) body.get("cons");
        Integer gameplayScore = toInt(body.get("gameplayScore"));
        Integer graphicsScore = toInt(body.get("graphicsScore"));
        Integer storyScore = toInt(body.get("storyScore"));
        Integer musicScore = toInt(body.get("musicScore"));

        Optional<GameReview> existing = reviewRepository.findByGameIdAndUserId(id, userId);
        GameReview review;
        if (existing.isPresent()) {
            review = existing.get();
        } else {
            review = new GameReview();
            review.setGame(new Game());
            review.getGame().setId(id);
            review.setUser(new User());
            review.getUser().setId(userId);
            review.setCreatedAt(new Timestamp(new Date().getTime()));
        }

        review.setText(text);
        review.setPros(pros);
        review.setCons(cons);
        review.setGameplayScore(gameplayScore);
        review.setGraphicsScore(graphicsScore);
        review.setStoryScore(storyScore);
        review.setMusicScore(musicScore);
        review.setUpdatedAt(new Timestamp(new Date().getTime()));
        reviewRepository.save(review);

        Map<String, Object> m = buildReviewMap(review, userId);
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @DeleteMapping("/{gameId}/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long gameId,
            @PathVariable Long reviewId) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        var opt = reviewRepository.findById(reviewId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        GameReview review = opt.get();
        if (!userId.equals(review.getUser().getId()) && !isAdmin()) {
            return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        }
        reviewRepository.delete(review);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Map<String, Object> buildReviewMap(GameReview r, Long currentUserId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("text", r.getText());
        m.put("pros", r.getPros());
        m.put("cons", r.getCons());
        m.put("gameplayScore", r.getGameplayScore());
        m.put("graphicsScore", r.getGraphicsScore());
        m.put("storyScore", r.getStoryScore());
        m.put("musicScore", r.getMusicScore());
        m.put("userId", r.getUser().getId());
        m.put("username", r.getUser().getUsername());
        m.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
        m.put("canDelete", currentUserId != null && (currentUserId.equals(r.getUser().getId()) || isAdmin()));
        return m;
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
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

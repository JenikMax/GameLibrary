package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.RecommendationService;
import com.jenikmax.game.library.service.data.api.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Recommendations", description = "AI-powered game recommendations: content-based and personalized")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);

    private final RecommendationService recommendationService;
    private final UserService userService;

    public RecommendationController(RecommendationService recommendationService, UserService userService) {
        this.recommendationService = recommendationService;
        this.userService = userService;
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkAvailable() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("available", recommendationService.isAvailable())));
    }

    @GetMapping("/similar/{gameId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSimilarGames(
            @PathVariable Long gameId,
            @RequestParam(defaultValue = "10") int limit) {
        if (!recommendationService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.error("AI recommendations are not available"));
        }
        List<Map<String, Object>> result = recommendationService.getContentBasedSimilar(gameId, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/for-you")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getForYou(
            @RequestParam(defaultValue = "10") int limit) {
        if (!recommendationService.isAvailable()) {
            return ResponseEntity.ok(ApiResponse.error("AI recommendations are not available"));
        }
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.error("User not authenticated"));
        }
        List<Map<String, Object>> result = recommendationService.getUserRecommendations(userId, limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.FavoriteGameRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.FavoriteGame;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/games")
public class FavoriteController {

    private final FavoriteGameRepository favoriteRepository;
    private final UserService userService;

    public FavoriteController(FavoriteGameRepository favoriteRepository, UserService userService) {
        this.favoriteRepository = favoriteRepository;
        this.userService = userService;
    }

    @GetMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFavoriteStatus(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean favorited = userId != null && favoriteRepository.existsByUserIdAndGameId(userId, id);
        long count = favoriteRepository.countByGameId(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", favorited, "favoritesCount", count)));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleFavorite(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }

        var existing = favoriteRepository.findByUserIdAndGameId(userId, id);
        boolean nowFavorited;
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            nowFavorited = false;
        } else {
            FavoriteGame fg = new FavoriteGame();
            fg.setUser(new User());
            fg.getUser().setId(userId);
            fg.setGame(new Game());
            fg.getGame().setId(id);
            fg.setCreatedAt(new Timestamp(new Date().getTime()));
            favoriteRepository.save(fg);
            nowFavorited = true;
        }

        long count = favoriteRepository.countByGameId(id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", nowFavorited, "favoritesCount", count)));
    }

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFavorites() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        List<Long> ids = favoriteRepository.findGameIdsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("ids", ids)));
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        var userDto = userService.getUserInfoByName(auth.getName());
        return userDto != null ? userDto.getId() : null;
    }
}

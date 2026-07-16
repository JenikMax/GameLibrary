package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.GameCollectionEntryRepository;
import com.jenikmax.game.library.dao.api.GameCollectionRepository;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.entity.GameCollection;
import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.CollectionService;
import com.jenikmax.game.library.service.data.api.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final GameCollectionRepository collectionRepository;
    private final GameCollectionEntryRepository entryRepository;
    private final UserService userService;

    public CollectionController(CollectionService collectionService,
                                 GameCollectionRepository collectionRepository,
                                 GameCollectionEntryRepository entryRepository,
                                 UserService userService) {
        this.collectionService = collectionService;
        this.collectionRepository = collectionRepository;
        this.entryRepository = entryRepository;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCollections() {
        Long userId = getCurrentUserId();
        List<GameCollection> mine = collectionService.getUserCollections(userId);
        List<GameCollection> shared = collectionService.getPublicCollections();

        Set<Long> seen = new HashSet<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameCollection c : mine) {
            result.add(toMap(c));
            seen.add(c.getId());
        }
        for (GameCollection c : shared) {
            if (!seen.contains(c.getId())) {
                result.add(toMap(c));
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/with-hero")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listCollectionsWithHero() {
        Long currentUserId = getCurrentUserId();
        List<Map<String, Object>> result = collectionService.getCollectionsWithHeroData(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCollection(@PathVariable Long id) {
        return collectionService.getById(id)
                .map(c -> ResponseEntity.ok(ApiResponse.ok(toMap(c))))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", "");
        boolean isPublic = Boolean.TRUE.equals(body.get("isPublic"));
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name is required"));
        }
        Long userId = getCurrentUserId();
        if (userId == null) return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        GameCollection c = collectionService.create(name, description, isPublic, userId);
        return ResponseEntity.ok(ApiResponse.ok(toMap(c)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(@PathVariable Long id,
                                                                    @RequestBody Map<String, Object> body) {
        if (!canModify(id)) return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        String name = (String) body.get("name");
        String description = (String) body.getOrDefault("description", "");
        boolean isPublic = Boolean.TRUE.equals(body.get("isPublic"));
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Name is required"));
        }
        GameCollection c = collectionService.update(id, name, description, isPublic);
        return ResponseEntity.ok(ApiResponse.ok(toMap(c)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        if (!canModify(id)) return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        collectionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    @GetMapping("/{id}/games")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getGames(@PathVariable Long id) {
        List<GameCollectionEntry> entries = collectionService.getEntries(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (GameCollectionEntry e : entries) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("gameId", e.getGameId());
            m.put("sortOrder", e.getSortOrder());
            m.put("addedAt", e.getAddedAt() != null ? e.getAddedAt().toString() : null);
            result.add(m);
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{id}/games")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addGame(@PathVariable Long id,
                                                                     @RequestBody Map<String, Object> body) {
        if (!canModify(id)) return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        Long gameId = body.get("gameId") instanceof Number n ? n.longValue() : null;
        if (gameId == null) return ResponseEntity.badRequest().body(ApiResponse.error("gameId is required"));
        GameCollectionEntry e = collectionService.addGame(id, gameId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("gameId", e.getGameId());
        m.put("sortOrder", e.getSortOrder());
        return ResponseEntity.ok(ApiResponse.ok(m));
    }

    @DeleteMapping("/{id}/games/{gameId}")
    public ResponseEntity<ApiResponse<Void>> removeGame(@PathVariable Long id, @PathVariable Long gameId) {
        if (!canModify(id)) return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        collectionService.removeGame(id, gameId);
        return ResponseEntity.ok(ApiResponse.ok("Removed", null));
    }

    @PutMapping("/{id}/games/reorder")
    public ResponseEntity<ApiResponse<Void>> reorder(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!canModify(id)) return ResponseEntity.status(403).body(ApiResponse.error("Forbidden"));
        @SuppressWarnings("unchecked")
        List<Integer> order = (List<Integer>) body.get("order");
        if (order == null) return ResponseEntity.badRequest().body(ApiResponse.error("order is required"));
        List<Long> gameIds = order.stream().map(Integer::longValue).toList();
        collectionService.reorder(id, gameIds);
        return ResponseEntity.ok(ApiResponse.ok("Reordered", null));
    }

    private Map<String, Object> toMap(GameCollection c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("name", c.getName());
        m.put("description", c.getDescription());
        m.put("userId", c.getUser().getId());
        m.put("username", c.getUser().getUsername());
        m.put("isPublic", c.getIsPublic());
        m.put("gameCount", collectionService.getEntryCount(c.getId()));
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        m.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
        return m;
    }

    private boolean canModify(Long collectionId) {
        Long userId = getCurrentUserId();
        boolean isAdmin = isCurrentUserAdmin();
        return isAdmin || collectionService.isOwner(collectionId, userId);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userService.getUserInfoByName(auth.getName()).getId();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Long id = userService.getUserInfoByName(auth.getName()).getId();
        User u = new User();
        u.setId(id);
        return u;
    }

    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}

package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.UserAdminResponse;
import com.jenikmax.game.library.service.data.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin", description = "User administration (admin only)")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserDataService userService;

    public AdminController(UserDataService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserAdminResponse>>> getUsers() {
        List<UserDto> users = userService.getAllUsers();
        List<UserAdminResponse> items = users.stream()
                .map(this::toUserAdminResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping("/users/{id}/toggle-admin")
    public ResponseEntity<ApiResponse<Void>> toggleAdmin(@PathVariable Long id,
                                                           @RequestParam boolean isAdmin) {
        logger.info("REST toggle admin for user {}: {}", id, isAdmin);
        try {
            userService.changeUserPrivilegy(id, isAdmin);
            return ResponseEntity.ok(ApiResponse.ok("User privileges updated", null));
        } catch (Exception e) {
            logger.error("Toggle admin error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleActive(@PathVariable Long id,
                                                           @RequestParam boolean isActive) {
        logger.info("REST toggle active for user {}: {}", id, isActive);
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            userService.changeUserActivity(id, isActive, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok("User status updated", null));
        } catch (Exception e) {
            logger.error("Toggle active error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/users/{id}/reset-pass")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable Long id) {
        logger.info("REST reset password for user {}", id);
        try {
            String newPassword = userService.resetUserPass(id);
            return ResponseEntity.ok(ApiResponse.ok("Password reset successful", newPassword));
        } catch (Exception e) {
            logger.error("Reset password error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to reset password"));
        }
    }

    private UserAdminResponse toUserAdminResponse(UserDto dto) {
        UserAdminResponse resp = new UserAdminResponse();
        resp.setId(dto.getId());
        resp.setName(dto.getName());
        resp.setAdmin(dto.isAdmin());
        resp.setActive(dto.isActive());
        resp.setAvatarUrl(avatarUrl(dto));
        return resp;
    }

    private static String avatarUrl(ShortUser u) {
        int v = u.getAvatar() != null ? u.getAvatar().hashCode() : 0;
        return "/game-library/api/images/avatars/" + u.getId() + "?v=" + v;
    }

}

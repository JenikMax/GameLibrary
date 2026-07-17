package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.PasswordChangeRequest;
import com.jenikmax.game.library.model.dto.api.ProfileUpdateRequest;
import com.jenikmax.game.library.model.dto.api.UserProfileResponse;
import com.jenikmax.game.library.service.data.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Profile", description = "User profile management")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final UserDataService userService;

    public ProfileController(UserDataService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ShortUser shortUser = userService.getUserInfoByName(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(toProfileResponse(shortUser)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ShortUser currentUser = userService.getUserInfoByName(auth.getName());

        if (request.getAvatar() == null || request.getAvatar().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok(toProfileResponse(currentUser)));
        }

        UserDto userDto = new UserDto();
        userDto.setId(currentUser.getId());
        userDto.setAvatar(request.getAvatar());
        try {
            userService.updateUser(userDto);
            ShortUser updated = userService.getUserInfoByName(auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(toProfileResponse(updated)));
        } catch (Exception e) {
            logger.error("Profile update error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update profile"));
        }
    }

    @PostMapping("/pass")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ShortUser currentUser = userService.getUserInfoByName(auth.getName());
        UserDto userDto = new UserDto();
        userDto.setId(currentUser.getId());
        userDto.setPass(request.getNewPassword());
        try {
            userService.updateUserPass(userDto);
            return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
        } catch (Exception e) {
            logger.error("Password change error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to change password"));
        }
    }

    private UserProfileResponse toProfileResponse(ShortUser shortUser) {
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(shortUser.getId());
        profile.setName(shortUser.getName());
        profile.setAdmin(shortUser.isAdmin());
        profile.setActive(shortUser.isActive());
        profile.setAvatarUrl(avatarUrl(shortUser));
        return profile;
    }

    static String avatarUrl(ShortUser u) {
        int v = u.getAvatar() != null ? u.getAvatar().hashCode() : 0;
        return "/game-library/api/images/avatars/" + u.getId() + "?v=" + v;
    }

}

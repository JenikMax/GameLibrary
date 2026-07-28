package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.*;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.UserDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.PasswordChangeRequest;
import com.jenikmax.game.library.model.dto.api.ProfileUpdateRequest;
import com.jenikmax.game.library.model.dto.api.UserProfileResponse;
import com.jenikmax.game.library.model.entity.User;
import com.jenikmax.game.library.service.data.UserDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/profile")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Profile", description = "User profile management")
/**
 * Контроллер управления профилем пользователя.
 * Обрабатывает запросы по пути /api/profile.
 * Предоставляет получение и обновление профиля, смену пароля,
 * а также агрегированную статистику пользователя (количество игр,
 * рейтингов, рецензий, комментариев, избранного, коллекций).
 */
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final UserDataService userService;
    private final UserRepository userRepository;
    private final GameRatingRepository gameRatingRepository;
    private final GameReviewRepository gameReviewRepository;
    private final GameCommentRepository gameCommentRepository;
    private final FavoriteGameRepository favoriteGameRepository;
    private final GameCollectionRepository gameCollectionRepository;
    private final JdbcTemplate jdbcTemplate;

    public ProfileController(UserDataService userService,
                             UserRepository userRepository,
                             GameRatingRepository gameRatingRepository,
                             GameReviewRepository gameReviewRepository,
                             GameCommentRepository gameCommentRepository,
                             FavoriteGameRepository favoriteGameRepository,
                             GameCollectionRepository gameCollectionRepository,
                             JdbcTemplate jdbcTemplate) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.gameRatingRepository = gameRatingRepository;
        this.gameReviewRepository = gameReviewRepository;
        this.gameCommentRepository = gameCommentRepository;
        this.favoriteGameRepository = favoriteGameRepository;
        this.gameCollectionRepository = gameCollectionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Получить профиль текущего пользователя со статистикой.
     * @return профиль пользователя и агрегированные счётчики
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ShortUser shortUser = userService.getUserInfoByName(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(toProfileResponse(shortUser)));
    }

    /**
     * Обновить аватар текущего пользователя.
     * @param request запрос с новым аватаром (base64)
     * @return обновлённый профиль
     */
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

    /**
     * Сменить пароль текущего пользователя.
     * @param request запрос с новым паролем
     * @return сообщение об успешной смене пароля
     */
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

        Long userId = shortUser.getId();

        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getCreateTs() != null) {
            LocalDate created = user.getCreateTs().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            profile.setMemberSince(String.valueOf(created.getYear()));
        } else {
            profile.setMemberSince(null);
        }

        try {
            Long totalGames = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library.game_data", Long.class);
            profile.setGamesCount(totalGames != null ? totalGames : 0);
        } catch (Exception e) {
            profile.setGamesCount(0);
        }

        profile.setRatingsCount(gameRatingRepository.countByUserId(userId));
        profile.setCollectionsCount(gameCollectionRepository.countByUserId(userId));
        profile.setReviewsCount(gameReviewRepository.countByUserId(userId));
        profile.setCommentsCount(gameCommentRepository.countByUserId(userId));
        profile.setFavoritesCount(favoriteGameRepository.countByUserId(userId));

        return profile;
    }

    static String avatarUrl(ShortUser u) {
        int v = u.getAvatar() != null ? u.getAvatar().hashCode() : 0;
        return "/game-library/api/images/avatars/" + u.getId() + "?v=" + v;
    }

}
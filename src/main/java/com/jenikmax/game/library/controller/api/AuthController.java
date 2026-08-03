package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.dto.api.*;
import com.jenikmax.game.library.model.exceptions.IllegalPassException;
import com.jenikmax.game.library.model.exceptions.IllegalUsernameException;
import com.jenikmax.game.library.service.data.UserDataService;
import com.jenikmax.game.library.config.jwt.JwtTokenProvider;
import com.jenikmax.game.library.config.jwt.RefreshTokenBlacklist;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth", description = "Authentication and registration")
/**
 * Контроллер аутентификации и регистрации.
 * Обрабатывает запросы по пути /api/auth.
 * Предоставляет endpoints для входа (JWT + httpOnly cookie), регистрации новых
 * пользователей и получения информации о текущем аутентифицированном пользователе.
 * Не требует предварительной аутентификации (кроме /me).
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDataService userService;
    private final RefreshTokenBlacklist blacklist;
    private final long accessExpirationMs;
    private final long refreshExpirationMs;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, UserDataService userService,
                           RefreshTokenBlacklist blacklist,
                           @Value("${game-library.jwt.access-expiration-ms:900000}") long accessExpirationMs,
                           @Value("${game-library.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
        this.blacklist = blacklist;
        this.accessExpirationMs = accessExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Аутентификация пользователя. При успешном входе возвращает JWT-токен,
     * а также устанавливает httpOnly cookie с токеном.
     * @param loginRequest запрос с именем пользователя и паролем
     * @param response     HTTP-ответ для установки cookie
     * @return объект с JWT-токеном и профилем пользователя
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest,
                                                             HttpServletRequest request, HttpServletResponse response) {
        logger.info("REST login request for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String accessToken = tokenProvider.generateAccessToken(authentication);
            String refreshToken = tokenProvider.generateRefreshToken(authentication);

            Cookie accessCookie = new Cookie("token", accessToken);
            accessCookie.setHttpOnly(true);
            accessCookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
            accessCookie.setPath("/game-library");
            accessCookie.setMaxAge((int) (accessExpirationMs / 1000));
            response.addCookie(accessCookie);

            Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
            refreshCookie.setPath("/game-library/api/auth");
            refreshCookie.setMaxAge((int) (refreshExpirationMs / 1000));
            response.addCookie(refreshCookie);

            com.jenikmax.game.library.model.dto.ShortUser shortUser = userService.getUserInfoByName(loginRequest.getUsername());
            UserProfileResponse profile = new UserProfileResponse();
            profile.setId(shortUser.getId());
            profile.setName(shortUser.getName());
            profile.setAdmin(shortUser.isAdmin());
            profile.setActive(shortUser.isActive());
            profile.setAvatarUrl(avatarUrl(shortUser));

            LoginResponse loginResponse = new LoginResponse(accessToken, profile);
            return ResponseEntity.ok(ApiResponse.ok(loginResponse));
        } catch (Exception e) {
            logger.warn("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }

    /**
     * Регистрация нового пользователя.
     * @param registerRequest запрос с именем пользователя и паролем
     * @return сообщение об успешной регистрации или ошибку валидации
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("REST register request for user: {}", registerRequest.getUsername());
        try {
            com.jenikmax.game.library.model.dto.RegistrationForm form = new com.jenikmax.game.library.model.dto.RegistrationForm();
            form.setUsername(registerRequest.getUsername());
            form.setPassword(registerRequest.getPassword());
            userService.registerUser(form);
            return ResponseEntity.ok(ApiResponse.ok("Registration successful", null));
        } catch (IllegalPassException | IllegalUsernameException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("Registration failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Registration failed"));
        }
    }

    /**
     * Получить профиль текущего аутентифицированного пользователя.
     * @return профиль пользователя (ID, имя, роль, статус, URL аватара)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        com.jenikmax.game.library.model.dto.ShortUser shortUser = userService.getUserInfoByName(auth.getName());
        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(shortUser.getId());
        profile.setName(shortUser.getName());
        profile.setAdmin(shortUser.isAdmin());
        profile.setActive(shortUser.isActive());
        profile.setAvatarUrl(avatarUrl(shortUser));
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("No refresh token"));
        }
        String refreshToken = Arrays.stream(cookies)
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst().orElse(null);
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid refresh token"));
        }
        if (!"REFRESH".equals(tokenProvider.getTokenType(refreshToken))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid token type"));
        }
        String jti = tokenProvider.getJtiFromToken(refreshToken);
        if (blacklist.isBlacklisted(jti)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Token revoked"));
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        com.jenikmax.game.library.model.dto.ShortUser shortUser = userService.getUserInfoByName(username);
        if (shortUser == null || !shortUser.isActive()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("User not found or inactive"));
        }

        String newAccessToken = tokenProvider.generateAccessTokenInternal(username);

        Cookie accessCookie = new Cookie("token", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")));
        accessCookie.setPath("/game-library");
        accessCookie.setMaxAge((int) (accessExpirationMs / 1000));
        response.addCookie(accessCookie);

        UserProfileResponse profile = new UserProfileResponse();
        profile.setId(shortUser.getId());
        profile.setName(shortUser.getName());
        profile.setAdmin(shortUser.isAdmin());
        profile.setActive(shortUser.isActive());
        profile.setAvatarUrl(avatarUrl(shortUser));

        return ResponseEntity.ok(ApiResponse.ok(new LoginResponse(newAccessToken, profile)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String refreshToken = Arrays.stream(cookies)
                    .filter(c -> "refresh_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst().orElse(null);
            if (refreshToken != null && tokenProvider.validateToken(refreshToken)) {
                String jti = tokenProvider.getJtiFromToken(refreshToken);
                blacklist.blacklist(jti, tokenProvider.getExpirationFromToken(refreshToken).toInstant());
            }
        }

        Cookie clearAccess = new Cookie("token", "");
        clearAccess.setHttpOnly(true);
        clearAccess.setPath("/game-library");
        clearAccess.setMaxAge(0);
        response.addCookie(clearAccess);

        Cookie clearRefresh = new Cookie("refresh_token", "");
        clearRefresh.setHttpOnly(true);
        clearRefresh.setPath("/game-library/api/auth");
        clearRefresh.setMaxAge(0);
        response.addCookie(clearRefresh);

        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    private static String avatarUrl(ShortUser u) {
        int v = u.getAvatar() != null ? u.getAvatar().hashCode() : 0;
        return "/game-library/api/images/avatars/" + u.getId() + "?v=" + v;
    }

}

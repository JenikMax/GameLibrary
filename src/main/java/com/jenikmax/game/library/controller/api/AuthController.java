package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.*;
import com.jenikmax.game.library.model.exceptions.IllegalPassException;
import com.jenikmax.game.library.model.exceptions.IllegalUsernameException;
import com.jenikmax.game.library.service.data.UserDataService;
import com.jenikmax.game.library.config.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDataService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider, UserDataService userService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("REST login request for user: {}", loginRequest.getUsername());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            com.jenikmax.game.library.model.dto.ShortUser shortUser = userService.getUserInfoByName(loginRequest.getUsername());
            UserProfileResponse profile = new UserProfileResponse();
            profile.setId(shortUser.getId());
            profile.setName(shortUser.getName());
            profile.setAdmin(shortUser.isAdmin());
            profile.setActive(shortUser.isActive());
            profile.setAvatarUrl("/game-library/api/images/avatars/" + shortUser.getId());

            LoginResponse loginResponse = new LoginResponse(token, profile);
            return ResponseEntity.ok(ApiResponse.ok(loginResponse));
        } catch (Exception e) {
            logger.warn("Login failed for user {}: {}", loginRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        }
    }

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
        profile.setAvatarUrl("/game-library/api/images/avatars/" + shortUser.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}

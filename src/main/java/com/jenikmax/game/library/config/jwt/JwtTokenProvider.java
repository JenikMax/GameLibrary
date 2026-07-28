package com.jenikmax.game.library.config.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Провайдер JWT-токенов: генерация, парсинг, валидация.
 * Использует HMAC-SHA (алгоритм выбирается по длине ключа).
 * Срок жизни токена — из конфигурации (по умолч. 24ч).
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    /** Секретный ключ подписи JWT. */
    private final SecretKey key;
    /** Время жизни токена в миллисекундах. */
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${game-library.jwt.secret}") String secret,
            @Value("${game-library.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Создать JWT-токен для аутентифицированного пользователя. */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /** Извлечь имя пользователя из JWT-токена. */
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /** Проверить валидность JWT-токена (подпись + срок). */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            logger.warn("Невалидный JWT-токен: {}", ex.getMessage());
            return false;
        }
    }
}

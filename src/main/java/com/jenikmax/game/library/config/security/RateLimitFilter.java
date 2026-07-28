package com.jenikmax.game.library.config.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Фильтр ограничения запросов (rate limiting) на основе bucket4j.
 * Login: 5 запросов в минуту на IP+User-Agent.
 * API: 100 запросов в минуту на IP.
 * Возвращает HTTP 429 при превышении.
 */
@Order(1799)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);
    /** Хранилище bucket'ов в памяти (ключ — IP+UA / IP). */
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Лимит для /api/auth/login: 5 запросов в минуту. */
    private static final int LOGIN_MAX_REQUESTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);
    /** Глобальный лимит для /api/: 100 запросов в минуту на IP. */
    private static final int GLOBAL_API_MAX = 100;
    private static final Duration GLOBAL_API_WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Лимит на логин — по IP + User-Agent
        if (path.contains("/api/auth/login")) {
            String key = request.getRemoteAddr() + ":" + request.getHeader("User-Agent");
            Bucket bucket = buckets.computeIfAbsent("login:" + key, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(LOGIN_MAX_REQUESTS, Refill.intervally(LOGIN_MAX_REQUESTS, LOGIN_WINDOW)))
                            .build());
            if (!bucket.tryConsume(1)) {
                logger.warn("Превышен лимит запросов для login: {}", key);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"success\":false,\"message\":\"Too many requests. Try again in 1 minute.\"}");
                return;
            }
        }

        // Глобальный лимит API — по IP
        if (path.contains("/api/")) {
            String key = request.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent("api:" + key, k ->
                    Bucket.builder()
                            .addLimit(Bandwidth.classic(GLOBAL_API_MAX, Refill.intervally(GLOBAL_API_MAX, GLOBAL_API_WINDOW)))
                            .build());
            bucket.tryConsume(1);
        }

        chain.doFilter(request, response);
    }
}

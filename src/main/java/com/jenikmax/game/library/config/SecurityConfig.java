package com.jenikmax.game.library.config;

import com.jenikmax.game.library.config.jwt.JwtAuthenticationEntryPoint;
import com.jenikmax.game.library.config.jwt.JwtAuthenticationFilter;
import com.jenikmax.game.library.config.security.CustomAccessDeniedHandler;
import com.jenikmax.game.library.config.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Конфигурация Spring Security: JWT-аутентификация, rate limiting, CORS, CSRF.
 * Двойная аутентификация: form login (Thymeleaf) + JWT (REST API).
 * Stateless-сессии, BCrypt для паролей, Content Security Policy.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(UserDetailsService userDetailsService,
                           JwtAuthenticationFilter jwtAuthenticationFilter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           @Value("${game-library.cors.allowed-origins:}") String corsAllowedOrigins) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.corsAllowedOrigins = corsAllowedOrigins.isBlank()
            ? List.of()
            : Arrays.asList(corsAllowedOrigins.split(","));
    }

    /** Игнорировать статические ресурсы в цепочке безопасности. */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/resources/**");
    }

    /**
     * Цепочка фильтров безопасности.
     * CORS, CSRF (только для не-API), Security Headers, JWT + RateLimit фильтры.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(63072000))
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")))
            .exceptionHandling(ex -> ex
                .accessDeniedHandler(new CustomAccessDeniedHandler())
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/images/**").permitAll()
                .requestMatchers("/api/tracker/**").permitAll()
                .requestMatchers("/api/notifications/subscribe").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").hasAnyRole("ADMIN", "USER")

                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/scan").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/games/{id}/edit").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/games/{id}/grab").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/embeddings/generate").hasRole("ADMIN")

                .requestMatchers("/api/**").hasAnyRole("ADMIN", "USER")

                .anyRequest().permitAll());

        http.addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** AuthenticationManager из стандартной конфигурации. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /** Провайдер аутентификации: UserDetailsService + BCrypt. */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** BCrypt-хэширование паролей. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Фильтр ограничения запросов (login: 5/мин, API: 100/мин). */
    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }

    /** CORS-конфигурация из переменной окружения CORS_ALLOWED_ORIGINS. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            configuration.setAllowedOrigins(corsAllowedOrigins);
        } else {
            // Если origins не заданы — same-origin (CORS-заголовки не отправляются)
            configuration.setAllowedOriginPatterns(Collections.emptyList());
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

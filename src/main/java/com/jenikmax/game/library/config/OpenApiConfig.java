package com.jenikmax.game.library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Swagger/OpenAPI.
 * Документация доступна по /swagger-ui.html, /v3/api-docs.
 * Разделена на группы: Public API и Admin API.
 */
@Configuration
public class OpenApiConfig {

    /** Глобальная конфигурация OpenAPI: заголовок, версия, JWT-авторизация. */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("GameLibrary API")
                        .version("1.0")
                        .description("REST API для приложения GameLibrary. Используйте JWT-токен от /api/auth/login в кнопке Authorize.")
                        .license(new License().name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT-токен, полученный от /api/auth/login")));
    }

    /** Группа публичных эндпоинтов (игры, авторизация, коллекции, статистика). */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .displayName("Public API")
                .pathsToMatch("/api/games/**", "/api/auth/**", "/api/collections/**", "/api/statistics/**", "/api/notifications/**", "/api/profile/**")
                .build();
    }

    /** Группа административных эндпоинтов (админка, сканирование). */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("admin")
                .displayName("Admin API")
                .pathsToMatch("/api/admin/**", "/api/scan/**")
                .build();
    }
}

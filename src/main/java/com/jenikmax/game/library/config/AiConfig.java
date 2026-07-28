package com.jenikmax.game.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация AI-сервиса (Python FastAPI).
 * Настройки из application.yml с префиксом 'ai'.
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    /** URL AI-сервиса (в Docker — ai-service:8000). */
    private String serviceUrl = "http://ai-service:8000";
    /** Конфигурация авто-тегирования. */
    private AutoTag autoTag = new AutoTag();

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public AutoTag getAutoTag() { return autoTag; }
    public void setAutoTag(AutoTag autoTag) { this.autoTag = autoTag; }

    /** Флаг включения/выключения авто-тегирования. */
    public static class AutoTag {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

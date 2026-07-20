package com.jenikmax.game.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai")
public class AiConfig {

    private String serviceUrl = "http://ai-service:8000";
    private AutoTag autoTag = new AutoTag();

    public String getServiceUrl() { return serviceUrl; }
    public void setServiceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; }
    public AutoTag getAutoTag() { return autoTag; }
    public void setAutoTag(AutoTag autoTag) { this.autoTag = autoTag; }

    public static class AutoTag {
        private boolean enabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}

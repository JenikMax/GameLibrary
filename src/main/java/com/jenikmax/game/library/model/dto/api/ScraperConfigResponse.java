package com.jenikmax.game.library.model.dto.api;

import com.jenikmax.game.library.service.scraper.model.ScraperConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScraperConfigResponse {

    private String type;
    private boolean enabled;
    private String displayName;
    private String baseUrl;
    private int timeoutMs;
    private int maxScreenshots;
    private String apiUrl;
    private String apiKey;
    private String authScheme;
    private boolean hasApiKey;

    private Map<String, String> cssSelectors;
    private Map<String, String> jsonPaths;
    private Map<String, List<String>> genreMappings;
    private Map<String, String> headers;

    private String sslProtocol;
    private boolean trustAllCerts;

    public static ScraperConfigResponse from(ScraperConfig cfg, String maskedKey, boolean hasKey) {
        ScraperConfigResponse r = new ScraperConfigResponse();
        r.type = cfg.getType();
        r.enabled = cfg.isEnabled();
        r.displayName = cfg.getDisplayName();
        r.baseUrl = cfg.getBaseUrl();
        r.timeoutMs = cfg.getTimeoutMs();
        r.maxScreenshots = cfg.getMaxScreenshots();
        r.apiUrl = cfg.getApiUrl();
        r.apiKey = maskedKey;
        r.authScheme = cfg.getAuthScheme();
        r.hasApiKey = hasKey;
        r.cssSelectors = cfg.getCssSelectors() != null ? new LinkedHashMap<>(cfg.getCssSelectors()) : null;
        r.jsonPaths = cfg.getJsonPaths() != null ? new LinkedHashMap<>(cfg.getJsonPaths()) : null;
        r.genreMappings = cfg.getGenreMappings() != null ? new LinkedHashMap<>(cfg.getGenreMappings()) : null;
        r.headers = cfg.getHeaders() != null ? new LinkedHashMap<>(cfg.getHeaders()) : null;
        r.sslProtocol = cfg.getSslProtocol();
        r.trustAllCerts = cfg.isTrustAllCerts();
        return r;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public int getMaxScreenshots() { return maxScreenshots; }
    public void setMaxScreenshots(int maxScreenshots) { this.maxScreenshots = maxScreenshots; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getAuthScheme() { return authScheme; }
    public void setAuthScheme(String authScheme) { this.authScheme = authScheme; }
    public boolean isHasApiKey() { return hasApiKey; }
    public void setHasApiKey(boolean hasApiKey) { this.hasApiKey = hasApiKey; }
    public Map<String, String> getCssSelectors() { return cssSelectors; }
    public void setCssSelectors(Map<String, String> cssSelectors) { this.cssSelectors = cssSelectors; }
    public Map<String, String> getJsonPaths() { return jsonPaths; }
    public void setJsonPaths(Map<String, String> jsonPaths) { this.jsonPaths = jsonPaths; }
    public Map<String, List<String>> getGenreMappings() { return genreMappings; }
    public void setGenreMappings(Map<String, List<String>> genreMappings) { this.genreMappings = genreMappings; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getSslProtocol() { return sslProtocol; }
    public void setSslProtocol(String sslProtocol) { this.sslProtocol = sslProtocol; }
    public boolean isTrustAllCerts() { return trustAllCerts; }
    public void setTrustAllCerts(boolean trustAllCerts) { this.trustAllCerts = trustAllCerts; }
}

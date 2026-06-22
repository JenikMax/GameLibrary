package com.jenikmax.game.library.service.scraper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ScraperConfig {

    private String type;
    private boolean enabled = true;
    private String displayName;
    private String baseUrl;
    private int timeoutMs = 10000;
    private int maxScreenshots = 20;

    private Map<String, String> cssSelectors;
    private Map<String, String> jsonPaths;
    private Map<String, List<String>> genreMappings;

    private String apiUrl;
    private String encryptedApiKey;
    private String authScheme;
    private Map<String, String> headers;

    private String sslProtocol = "TLSv1.2";
    private boolean trustAllCerts;

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
    public Map<String, String> getCssSelectors() { return cssSelectors; }
    public void setCssSelectors(Map<String, String> cssSelectors) { this.cssSelectors = cssSelectors; }
    public Map<String, String> getJsonPaths() { return jsonPaths; }
    public void setJsonPaths(Map<String, String> jsonPaths) { this.jsonPaths = jsonPaths; }
    public Map<String, List<String>> getGenreMappings() { return genreMappings; }
    public void setGenreMappings(Map<String, List<String>> genreMappings) { this.genreMappings = genreMappings; }
    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    @JsonProperty("apiKey")
    public String getEncryptedApiKey() { return encryptedApiKey; }
    @JsonProperty("apiKey")
    public void setEncryptedApiKey(String encryptedApiKey) { this.encryptedApiKey = encryptedApiKey; }
    public String getAuthScheme() { return authScheme; }
    public void setAuthScheme(String authScheme) { this.authScheme = authScheme; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public String getSslProtocol() { return sslProtocol; }
    public void setSslProtocol(String sslProtocol) { this.sslProtocol = sslProtocol; }
    public boolean isTrustAllCerts() { return trustAllCerts; }
    public void setTrustAllCerts(boolean trustAllCerts) { this.trustAllCerts = trustAllCerts; }
}

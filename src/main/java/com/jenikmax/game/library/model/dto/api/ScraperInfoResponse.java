package com.jenikmax.game.library.model.dto.api;

public class ScraperInfoResponse {

    private String type;
    private String displayName;

    public ScraperInfoResponse() {}

    public ScraperInfoResponse(String type, String displayName) {
        this.type = type;
        this.displayName = displayName;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}

package com.jenikmax.game.library.model.dto.api;

public class UserProfileResponse {

    private Long id;
    private String name;
    private boolean admin;
    private boolean active;
    private String avatarUrl;

    public UserProfileResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}

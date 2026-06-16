package com.jenikmax.game.library.model.dto.api;

public class LoginResponse {

    private String token;
    private String tokenType = "Bearer";
    private UserProfileResponse user;

    public LoginResponse() {}

    public LoginResponse(String token, UserProfileResponse user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public UserProfileResponse getUser() { return user; }
    public void setUser(UserProfileResponse user) { this.user = user; }
}

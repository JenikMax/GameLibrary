package com.jenikmax.game.library.model.dto.api;

/**
 * Запрос на обновление профиля пользователя.
 * Содержит аватар в формате Base64 data URI.
 */
public class ProfileUpdateRequest {

    private String avatar; // Base64 data URI

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}

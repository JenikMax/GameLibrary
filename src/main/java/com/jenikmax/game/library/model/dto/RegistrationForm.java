package com.jenikmax.game.library.model.dto;

/**
 * Форма регистрации пользователя (Thymeleaf).
 * Содержит имя пользователя и пароль.
 */
public class RegistrationForm {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

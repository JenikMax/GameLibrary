package com.jenikmax.game.library.model.dto;

/**
 * Расширенное DTO пользователя, наследующее ShortUser.
 * Добавляет поле пароля (используется в администрировании).
 */
public class UserDto extends ShortUser{

    private String pass;

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}

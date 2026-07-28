package com.jenikmax.game.library.model.exceptions;

/**
 * Исключение, выбрасываемое при недопустимом пароле
 * (например, несоответствие требованиям безопасности).
 */
public class IllegalPassException extends IllegalArgumentException{

    /**
     * @param msg сообщение об ошибке
     */
    public IllegalPassException(String msg) {
        super(msg);
    }
}

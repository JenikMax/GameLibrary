package com.jenikmax.game.library.model.exceptions;

/**
 * Исключение, выбрасываемое при недопустимом имени пользователя
 * (например, слишком короткое, содержит запрещённые символы).
 */
public class IllegalUsernameException extends IllegalArgumentException{

    /**
     * @param msg сообщение об ошибке
     */
    public IllegalUsernameException(String msg){
        super(msg);
    }
}

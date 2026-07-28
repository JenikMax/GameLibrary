package com.jenikmax.game.library.model.dto.api;

/**
 * Стандартный ответ API.
 * @param <T> тип данных в поле data
 */
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * Создаёт успешный ответ с данными.
     * @param data данные ответа
     * @return ApiResponse с success = true
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data);
    }

    /**
     * Создаёт успешный ответ с сообщением и данными.
     * @param message сообщение
     * @param data данные ответа
     * @return ApiResponse с success = true
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Создаёт ответ с ошибкой.
     * @param message сообщение об ошибке
     * @return ApiResponse с success = false
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}

package com.jenikmax.game.library.model.dto;

import com.jenikmax.game.library.model.entity.Notification;

import java.sql.Timestamp;

/**
 * DTO для отправки уведомления клиенту (через SSE или WebSocket).
 * Содержит все поля уведомления, кроме ссылки на пользователя.
 */
public class NotificationEvent {

    private Long id;
    private String type;
    private String title;
    private String message;
    private Long gameId;
    private boolean read;
    private Timestamp createdAt;

    public NotificationEvent() {}

    /**
     * Создаёт DTO из сущности Notification.
     * @param n сущность уведомления
     * @return DTO для передачи клиенту
     */
    public static NotificationEvent from(Notification n) {
        NotificationEvent e = new NotificationEvent();
        e.setId(n.getId());
        e.setType(n.getType());
        e.setTitle(n.getTitle());
        e.setMessage(n.getMessage());
        e.setGameId(n.getGameId());
        e.setRead(n.isRead());
        e.setCreatedAt(n.getCreatedAt());
        return e;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

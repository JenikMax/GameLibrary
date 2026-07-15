package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "notification")
@SequenceGenerator(
        name = "notification_id_gen",
        allocationSize = 1,
        sequenceName = "notification_id_seq")
public class Notification implements Serializable {

    private Long id;
    private User user;
    private String type;
    private String title;
    private String message;
    private Long gameId;
    private boolean isRead;
    private Timestamp createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_gen")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Column(name = "type")
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Column(name = "title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @Column(name = "message", columnDefinition = "text")
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Column(name = "game_id")
    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }

    @Column(name = "is_read")
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    @Column(name = "created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

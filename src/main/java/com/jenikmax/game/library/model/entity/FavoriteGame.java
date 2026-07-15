package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "favorite_game")
@SequenceGenerator(
        name = "favorite_game_id_gen",
        allocationSize = 1,
        sequenceName = "favorite_game_id_seq")
public class FavoriteGame implements Serializable {

    private Long id;
    private User user;
    private Game game;
    private Timestamp createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "favorite_game_id_gen")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @ManyToOne
    @JoinColumn(name = "game_id")
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    @Column(name = "created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

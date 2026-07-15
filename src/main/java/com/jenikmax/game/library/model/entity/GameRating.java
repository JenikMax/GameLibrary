package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "game_rating")
@SequenceGenerator(
        name = "game_rating_id_gen",
        allocationSize = 1,
        sequenceName = "game_rating_id_seq")
public class GameRating implements Serializable {

    private Long id;
    private Game game;
    private User user;
    private int rating;
    private Timestamp createdAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_rating_id_gen")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "game_id")
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Column(name = "rating")
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    @Column(name = "created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}

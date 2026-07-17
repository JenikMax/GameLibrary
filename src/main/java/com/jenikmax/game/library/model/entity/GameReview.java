package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(name = "game_review")
@SequenceGenerator(
        name = "game_review_id_gen",
        allocationSize = 1,
        sequenceName = "game_review_id_seq")
public class GameReview implements Serializable {

    private Long id;
    private Game game;
    private User user;
    private String text;
    private String pros;
    private String cons;
    private Integer gameplayScore;
    private Integer graphicsScore;
    private Integer storyScore;
    private Integer musicScore;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_review_id_gen")
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

    @Column(name = "text", columnDefinition = "text")
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    @Column(name = "pros", columnDefinition = "text")
    public String getPros() { return pros; }
    public void setPros(String pros) { this.pros = pros; }

    @Column(name = "cons", columnDefinition = "text")
    public String getCons() { return cons; }
    public void setCons(String cons) { this.cons = cons; }

    @Column(name = "gameplay_score")
    public Integer getGameplayScore() { return gameplayScore; }
    public void setGameplayScore(Integer gameplayScore) { this.gameplayScore = gameplayScore; }

    @Column(name = "graphics_score")
    public Integer getGraphicsScore() { return graphicsScore; }
    public void setGraphicsScore(Integer graphicsScore) { this.graphicsScore = graphicsScore; }

    @Column(name = "story_score")
    public Integer getStoryScore() { return storyScore; }
    public void setStoryScore(Integer storyScore) { this.storyScore = storyScore; }

    @Column(name = "music_score")
    public Integer getMusicScore() { return musicScore; }
    public void setMusicScore(Integer musicScore) { this.musicScore = musicScore; }

    @Column(name = "created_at")
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Column(name = "updated_at")
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}

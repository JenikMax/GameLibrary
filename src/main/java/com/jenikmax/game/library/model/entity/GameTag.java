package com.jenikmax.game.library.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "game_data_tag")
@SequenceGenerator(
        name = "game_data_tag_id_gen",
        allocationSize = 1,
        sequenceName = "game_data_tag_id_seq")
public class GameTag implements Serializable {

    private Long id;
    private Game game;
    private String tagCode;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_data_tag_id_gen")
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @ManyToOne
    @JoinColumn(name = "game_id")
    public Game getGame() { return game; }
    public void setGame(Game game) { this.game = game; }

    @Column(name = "tag_code", nullable = false)
    public String getTagCode() { return tagCode; }
    public void setTagCode(String tagCode) { this.tagCode = tagCode; }
}

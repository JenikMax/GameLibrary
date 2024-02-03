package com.jenikmax.game.library.model.entity;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "game_screenshot")
@SequenceGenerator(
        name="game_screenshot_id_gen",
        allocationSize=1,
        sequenceName="game_screenshot_id_seq")
public class Screenshot implements Serializable {
    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    private Game game;
    private String name;
    private byte[] source;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_screenshot_id_gen")
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "game_id")
    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    @Column(name = "name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "source")
    public byte[] getSource() {
        return source;
    }

    public void setSource(byte[] source) {
        this.source = source;
    }
}

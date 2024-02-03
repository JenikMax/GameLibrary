package com.jenikmax.game.library.model.entity;

import com.jenikmax.game.library.model.entity.enums.Genre;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "game_data_genre")
@SequenceGenerator(
        name="game_data_genre_id_gen",
        allocationSize=1,
        sequenceName="game_data_genre_id_seq")
public class GameGenre implements Serializable {

    @Column(name = "ID", unique = true, nullable = false)
    private Long id;

    private Game game;
    private Genre genre;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "game_data_genre_id_gen")
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

    @Column(name = "genre_code")
    @Enumerated(EnumType.STRING)
    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }
}

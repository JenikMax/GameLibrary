package com.jenikmax.game.library.model.dto;


public class GameGenreDto {

    private Long id;
    private Long gameId;
    private String genreCode;
    private String genreDescription;

    public GameGenreDto(){}

    public GameGenreDto(Long id, Long gameId, String genreCode, String genreDescription) {
        this.id = id;
        this.gameId = gameId;
        this.genreCode = genreCode;
        this.genreDescription = genreDescription;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getGenreCode() {
        return genreCode;
    }

    public void setGenreCode(String genreCode) {
        this.genreCode = genreCode;
    }

    public String getGenreDescription() {
        return genreDescription;
    }

    public void setGenreDescription(String genreDescription) {
        this.genreDescription = genreDescription;
    }
}

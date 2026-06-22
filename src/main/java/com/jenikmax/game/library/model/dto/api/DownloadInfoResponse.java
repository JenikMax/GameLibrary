package com.jenikmax.game.library.model.dto.api;

public class DownloadInfoResponse {

    private Long gameId;
    private String gameName;
    private String releaseDate;
    private String downloadUrl;

    public Long getGameId() { return gameId; }
    public void setGameId(Long gameId) { this.gameId = gameId; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
}

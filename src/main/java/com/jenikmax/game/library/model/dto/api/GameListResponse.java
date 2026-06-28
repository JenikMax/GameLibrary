package com.jenikmax.game.library.model.dto.api;

import java.sql.Timestamp;
import java.util.List;

public class GameListResponse {

    private Long id;
    private String name;
    private String platform;
    private String releaseDate;
    private List<String> genres;
    private String logoUrl;
    private String logo;

    public GameListResponse() {}

    public GameListResponse(Long id, String name, String platform, String releaseDate, List<String> genres, String logoUrl, String logo) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.releaseDate = releaseDate;
        this.genres = genres;
        this.logoUrl = logoUrl;
        this.logo = logo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
}

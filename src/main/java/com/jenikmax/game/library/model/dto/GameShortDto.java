package com.jenikmax.game.library.model.dto;

import java.sql.Timestamp;
import java.util.List;

public class GameShortDto {

    protected Long id;
    protected Timestamp createTs;
    protected String name;
    protected String directoryPath;
    protected String platform;
    protected String releaseDate;
    protected List<String> genres;
    protected String logo;

    public GameShortDto() {
    }

    public GameShortDto(Long id, Timestamp createTs, String name, String directoryPath, String platform, String releaseDate, List<String> genres, String logo) {
        this.id = id;
        this.createTs = createTs;
        this.name = name;
        this.directoryPath = directoryPath;
        this.platform = platform;
        this.releaseDate = releaseDate;
        this.genres = genres;
        this.logo = logo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Timestamp getCreateTs() {
        return createTs;
    }

    public void setCreateTs(Timestamp createTs) {
        this.createTs = createTs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }
}

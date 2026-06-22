package com.jenikmax.game.library.model.dto.api;

public class ScrapRequest {

    private String source;
    private String url;
    private boolean title;
    private boolean poster;
    private boolean description;
    private boolean year;
    private boolean genres;
    private boolean screens;

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public boolean isTitle() { return title; }
    public void setTitle(boolean title) { this.title = title; }
    public boolean isPoster() { return poster; }
    public void setPoster(boolean poster) { this.poster = poster; }
    public boolean isDescription() { return description; }
    public void setDescription(boolean description) { this.description = description; }
    public boolean isYear() { return year; }
    public void setYear(boolean year) { this.year = year; }
    public boolean isGenres() { return genres; }
    public void setGenres(boolean genres) { this.genres = genres; }
    public boolean isScreens() { return screens; }
    public void setScreens(boolean screens) { this.screens = screens; }
}

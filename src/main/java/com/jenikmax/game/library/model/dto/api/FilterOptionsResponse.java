package com.jenikmax.game.library.model.dto.api;

import java.util.List;

public class FilterOptionsResponse {

    private List<String> years;
    private List<String> platforms;
    private List<GenreItem> genres;

    public List<String> getYears() { return years; }
    public void setYears(List<String> years) { this.years = years; }
    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) { this.platforms = platforms; }
    public List<GenreItem> getGenres() { return genres; }
    public void setGenres(List<GenreItem> genres) { this.genres = genres; }

    public static class GenreItem {
        private String code;
        private String name;

        public GenreItem() {}
        public GenreItem(String code, String name) { this.code = code; this.name = name; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}

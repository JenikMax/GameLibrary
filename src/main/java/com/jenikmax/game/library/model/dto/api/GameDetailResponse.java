package com.jenikmax.game.library.model.dto.api;

import java.util.List;

public class GameDetailResponse {

    private Long id;
    private String name;
    private String platform;
    private String releaseDate;
    private String directoryPath;
    private List<String> genres;
    private List<String> tags;
    private String logoUrl;
    private String trailerUrl;
    private String description;
    private String instruction;
    private List<String> screenshotUrls;
    private String logo;
    private List<String> screenshots;
    private Double avgRating;
    private Integer userRating;
    private Long ratingsCount;
    private boolean favorited;

    public GameDetailResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public List<String> getScreenshotUrls() { return screenshotUrls; }
    public void setScreenshotUrls(List<String> screenshotUrls) { this.screenshotUrls = screenshotUrls; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public List<String> getScreenshots() { return screenshots; }
    public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }
    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
    public Integer getUserRating() { return userRating; }
    public void setUserRating(Integer userRating) { this.userRating = userRating; }
    public Long getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(Long ratingsCount) { this.ratingsCount = ratingsCount; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }
}

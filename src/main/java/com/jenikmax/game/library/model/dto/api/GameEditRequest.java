package com.jenikmax.game.library.model.dto.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class GameEditRequest {

    @NotBlank(message = "Game name is required")
    private String name;

    @NotBlank(message = "Platform is required")
    private String platform;
    private String releaseDate;
    private String directoryPath;
    private String description;
    private String instruction;
    private String trailerUrl;
    private List<String> genres;
    private String logo;
    private List<String> screenshots;
    private List<Long> deleteScreenshotIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public String getTrailerUrl() { return trailerUrl; }
    public void setTrailerUrl(String trailerUrl) { this.trailerUrl = trailerUrl; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }
    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }
    public List<String> getScreenshots() { return screenshots; }
    public void setScreenshots(List<String> screenshots) { this.screenshots = screenshots; }
    public List<Long> getDeleteScreenshotIds() { return deleteScreenshotIds; }
    public void setDeleteScreenshotIds(List<Long> deleteScreenshotIds) { this.deleteScreenshotIds = deleteScreenshotIds; }
}

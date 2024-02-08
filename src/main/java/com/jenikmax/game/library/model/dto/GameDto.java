package com.jenikmax.game.library.model.dto;

import java.sql.Timestamp;
import java.util.List;

public class GameDto extends GameShortDto {

    private String trailerUrl;
    private String description;
    private String instruction;
    private List<ScreenshotDto> screenshots;

    public GameDto(){}

    public GameDto(Long id, Timestamp createTs, String name, String directoryPath, String platform, String releaseDate, List<String> genres, String logo, String trailerUrl, String description, String instruction, List<ScreenshotDto> screenshots) {
        super(id, createTs, name, directoryPath, platform, releaseDate, genres, logo);
        this.trailerUrl = trailerUrl;
        this.description = description;
        this.instruction = instruction;
        this.screenshots = screenshots;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public List<ScreenshotDto> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<ScreenshotDto> screenshots) {
        this.screenshots = screenshots;
    }
}

package com.jenikmax.game.library.model.converter;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameGenreDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ScreenshotDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameGenre;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.ArrayList;
import java.util.Base64;

public class GameConverter {

    public static GameShortDto gameShortToDtoConverter(Game entity){
        GameShortDto dto = new GameShortDto();
        dto.setId(entity.getId());
        dto.setCreateTs(entity.getCreateTs());
        dto.setName(entity.getName());
        dto.setDirectoryPath(entity.getDirectoryPath());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setPlatform(entity.getPlatform());
        dto.setLogo("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(entity.getLogo()));
        dto.setGenres(new ArrayList<>());
        for (GameGenre gameGenre : entity.getGenres()){
            dto.getGenres().add(gameGenreToDtoConverter(gameGenre));
        }
        return dto;
    }

    public static Game dtoToGameEntityConverter(GameShortDto dto){
        Game entity = new Game();
        entity.setId(dto.getId());
        entity.setCreateTs(dto.getCreateTs());
        entity.setName(dto.getName());
        entity.setDirectoryPath(dto.getDirectoryPath());
        entity.setReleaseDate(dto.getReleaseDate());
        entity.setPlatform(dto.getPlatform());
        entity.setLogo(Base64.getDecoder().decode(dto.getLogo()));
        entity.setGenres(new ArrayList<>());
        for(GameGenreDto genreDto : dto.getGenres()){
            entity.getGenres().add(dtoToGameGenreEntityConverter(genreDto,entity));
        }
        return entity;
    }

    public static GameDto gameToDtoConverter(Game entity){
        GameDto dto = new GameDto();
        dto.setId(entity.getId());
        dto.setCreateTs(entity.getCreateTs());
        dto.setName(entity.getName());
        dto.setDirectoryPath(entity.getDirectoryPath());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setTrailerUrl(entity.getTrailerUrl());
        dto.setPlatform(entity.getPlatform());
        dto.setDescription(entity.getDescription());
        dto.setInstruction(entity.getInstruction());
        dto.setLogo("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(entity.getLogo()));
        dto.setScreenshots(new ArrayList<>());
        for (Screenshot screenshot : entity.getScreenshots()){
            dto.getScreenshots().add(screenshotToDtoConverter(screenshot));
        }
        dto.setGenres(new ArrayList<>());
        for (GameGenre gameGenre : entity.getGenres()){
            dto.getGenres().add(gameGenreToDtoConverter(gameGenre));
        }
        return dto;
    }

    public static Game dtoToGameEntityConverter(GameDto dto){
        Game entity = new Game();
        entity.setId(dto.getId());
        entity.setCreateTs(dto.getCreateTs());
        entity.setName(dto.getName());
        entity.setDirectoryPath(dto.getDirectoryPath());
        entity.setReleaseDate(dto.getReleaseDate());
        entity.setTrailerUrl(dto.getTrailerUrl());
        entity.setPlatform(dto.getPlatform());
        entity.setDescription(dto.getDescription());
        entity.setInstruction(dto.getInstruction());
        entity.setLogo(Base64.getDecoder().decode(dto.getLogo()));
        entity.setScreenshots(new ArrayList<>());
        for (ScreenshotDto screenshotDto : dto.getScreenshots()){
            entity.getScreenshots().add(dtoToScreenshotEntityConverter(screenshotDto,entity));
        }
        entity.setGenres(new ArrayList<>());
        for(GameGenreDto genreDto : dto.getGenres()){
            entity.getGenres().add(dtoToGameGenreEntityConverter(genreDto,entity));
        }
        return entity;
    }

    public static GameGenreDto gameGenreToDtoConverter(GameGenre entity){
        GameGenreDto dto = new GameGenreDto();
        dto.setId(entity.getId());
        dto.setGameId(entity.getGame().getId());
        dto.setGenreCode(entity.getGenre().toString());
        dto.setGenreDescription(entity.getGenre().getName());
        return dto;
    }

    public static GameGenre dtoToGameGenreEntityConverter(GameGenreDto dto, Game game){
        GameGenre entity = new GameGenre();
        entity.setId(dto.getId());
        entity.setGame(game);
        entity.setGenre(Genre.valueOf(dto.getGenreCode().toUpperCase()));
        return entity;
    }

    public static ScreenshotDto screenshotToDtoConverter(Screenshot entity){
        ScreenshotDto dto = new ScreenshotDto();
        dto.setId(entity.getId());
        dto.setGameId(entity.getGame().getId());
        dto.setName(entity.getName());
        dto.setSource("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(entity.getSource()));
        return dto;
    }

    public static Screenshot dtoToScreenshotEntityConverter(ScreenshotDto dto, Game game){
        Screenshot entity = new Screenshot();
        entity.setId(dto.getId());
        entity.setGame(game);
        entity.setName(dto.getName());
        entity.setSource(Base64.getDecoder().decode(dto.getSource()));
        return entity;
    }


}
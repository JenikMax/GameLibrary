package com.jenikmax.game.library.model.converter;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameGenre;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.ArrayList;
import java.util.Base64;

public class GameConverter {

    private final static String BASE_64_PREFIX = "data:image/jpeg;base64,";
    private final static String BASE_64_EMPTY = "data:image/jpeg;base64";

    public static GameShortDto gameShortToDtoConverter(Game entity){
        GameShortDto dto = new GameShortDto();
        dto.setId(entity.getId());
        dto.setCreateTs(entity.getCreateTs());
        dto.setName(entity.getName());
        dto.setDirectoryPath(entity.getDirectoryPath());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setPlatform(entity.getPlatform());
        dto.setLogo(BASE_64_PREFIX + Base64.getEncoder().encodeToString(entity.getLogo()));
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
        for(String genre: dto.getGenres()){
            entity.getGenres().add(dtoToGameGenreEntityConverter(genre,entity));
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
        dto.setLogo(BASE_64_PREFIX + Base64.getEncoder().encodeToString(entity.getLogo()));
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
        entity.setLogo(Base64.getDecoder().decode(dto.getLogo().replaceAll(BASE_64_PREFIX,"")));
        entity.setScreenshots(new ArrayList<>());
        if(dto.getScreenshots() != null){
            for (String screenshot : dto.getScreenshots()){
                if(!screenshot.equals(BASE_64_EMPTY)) entity.getScreenshots().add(dtoToScreenshotEntityConverter(screenshot,entity));
            }
        }
        entity.setGenres(new ArrayList<>());
        if(dto.getGenres() != null){
            for(String genre : dto.getGenres()){
                entity.getGenres().add(dtoToGameGenreEntityConverter(genre,entity));
            }
        }
        return entity;
    }

    public static String gameGenreToDtoConverter(GameGenre entity){
        //GameGenreDto dto = new GameGenreDto();
        //dto.setId(entity.getId());
        //dto.setGameId(entity.getGame().getId());
        //dto.setGenreCode(entity.getGenre().toString());
        //dto.setGenreDescription(entity.getGenre().getName());
        return entity.getGenre().toString();
    }

    public static GameGenre dtoToGameGenreEntityConverter(String genre, Game game){
        GameGenre entity = new GameGenre();
        entity.setGame(game);
        entity.setGenre(Genre.valueOf(genre));
        return entity;
    }

    public static String screenshotToDtoConverter(Screenshot entity){
        //ScreenshotDto dto = new ScreenshotDto();
        //dto.setId(entity.getId());
        //dto.setGameId(entity.getGame().getId());
        //dto.setName(entity.getName());
        //dto.setSource("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(entity.getSource()));
        return BASE_64_PREFIX + Base64.getEncoder().encodeToString(entity.getSource());
    }

    public static Screenshot dtoToScreenshotEntityConverter(String screenshot, Game game){
        Screenshot entity = new Screenshot();
        //entity.setId(dto.getId());
        entity.setGame(game);
        entity.setName("screenshot" + game.getScreenshots().size() + ".jpg");
        entity.setSource(Base64.getDecoder().decode(screenshot.replaceAll(BASE_64_PREFIX,"")));
        return entity;
    }


}

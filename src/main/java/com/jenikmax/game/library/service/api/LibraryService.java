package com.jenikmax.game.library.service.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;

import java.util.List;

public interface LibraryService {

    void scanLibrary();

    List<GameShortDto> getGameList();

    GameDto getGameInfo(Long gameId);

    GameDto updateGameInfo(GameDto gameDto);

    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<String> getGameGenres();


}

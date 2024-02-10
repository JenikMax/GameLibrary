package com.jenikmax.game.library.service.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;

import java.util.List;

public interface LibraryService {

    void scanLibrary();

    List<GameShortDto> getGameList();

    List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    GameDto getGameInfo(Long gameId);

    GameDto updateGameInfo(GameDto gameDto);

    GameDto grabGameInfo(Long id, String source);

    GameDto grabGameInfo(GameDto gameDto, String source);

    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<String> getGameGenres();


}

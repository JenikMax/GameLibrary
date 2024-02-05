package com.jenikmax.game.library.service.data.api;

import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;

import java.util.List;

public interface GameService {

    List<GameShortDto> getGameShortList();

    List<GameShortDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres);

    List<Game> getGameList();

    Game getGameById(Long gameId);

    void storeGame(Game game);

    void storeNewGameInLibrary(List<Game> games);


    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<String> getGameGenres();

}

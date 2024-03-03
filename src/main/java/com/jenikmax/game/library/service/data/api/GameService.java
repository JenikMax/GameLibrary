package com.jenikmax.game.library.service.data.api;

import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.List;
import java.util.Locale;

public interface GameService {

    List<GameReadDto> getGameShortList();

    List<GameReadDto> getGameShortList(int startIndex, int endIndex);

    List<Long> getGameShortIdList();

    List<GameReadDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    List<GameReadDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType, int startIndex, int endIndex);

    List<Long> getGameShortIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    List<Game> getGameList();

    Game getGameById(Long gameId);

    void storeGame(Game game);

    void deleteGameInfo(Long id);

    void updateGame(Game game);

    void storeNewGameInLibrary(List<Game> games);


    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<Genre> getGenres();

    List<Genre> getGenres(Locale locale);

    List<String> getGameGenres();

    byte[] getImageBytesById(Long id);

    byte[] getPosterBytesById(Long id);
}

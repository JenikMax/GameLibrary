package com.jenikmax.game.library.service.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.data.UserDataService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface LibraryService {

    void scanLibrary();

    List<GameShortDto> getGameList();

    List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    GameDto getGameInfo(Long gameId);

    GameDto updateGameInfo(GameDto gameDto);

    GameDto grabGameInfo(Long id, String source, String url);

    GameDto grabGameInfo(GameDto gameDto, String source);

    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<Genre> getGenres();

    List<Genre> getGenres(GameDto gameDto);

    List<String> getGameGenres();

    ResponseEntity<Resource> downloadGame(GameDto game);

    ShortUser getUserInfo();

}

package com.jenikmax.game.library.service.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public interface LibraryService {

    void scanLibrary();

    List<GameShortDto> getGameList();

    List<GameShortDto> getGameList(int startIndex, int endIndex);

    List<Long> getGameListId();

    List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType, int startIndex, int endIndex);

    List<Long> getGameIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType);

    GameDto getGameInfo(Long gameId);

    GameDto updateGameInfo(GameDto gameDto);

    GameDto grabGameInfo(Long id, String source, String url);

    GameDto grabGameInfo(Long id, ScrapInfo scrapInfo);

    GameDto grabGameInfo(GameDto gameDto, String source);

    List<String> getReleaseDates();

    List<String> getGamesPlatforms();

    List<Genre> getGenres();

    List<Genre> getGenres(Locale locale);

    List<Genre> getGenres(GameDto gameDto);

    List<String> getGameGenres();

    ResponseEntity<Resource> downloadGame(GameDto game);

    CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadGameInStream(GameDto game, HttpServletResponse response);

    ShortUser getUserInfo();

}

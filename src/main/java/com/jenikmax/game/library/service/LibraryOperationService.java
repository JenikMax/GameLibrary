package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryOperationService implements LibraryService {

    private final String rootDirectory;
    private final static String LIBRARY_PREFIX = "/games";

    private final GameService gameService;
    private final ScanerService scanerService;

    public LibraryOperationService(@Value("${game-library.games.directory}") String rootDirectory,
                                   GameService gameService, ScanerService scanerService) {
        this.rootDirectory = rootDirectory;
        this.gameService = gameService;
        this.scanerService = scanerService;
    }

    @Override
    public void scanLibrary() {
        List<Game> findGames = scanerService.scanDirectory(rootDirectory + LIBRARY_PREFIX);
        List<Game> storedGames = gameService.getGameList();
        List<Game> newGames = storedGames.isEmpty() ?
                findGames :
                findGames.stream().
                        filter(newGame -> storedGames.stream().
                                noneMatch(storedGame ->
                                        storedGame.getDirectoryPath().equals(newGame.getDirectoryPath())))
                        .collect(Collectors.toList());
        for(Game gameShort : newGames){
            Game game = scanerService.getAdditinalGameInfo(gameShort);
            gameService.storeGame(game);
        }
    }

    @Override
    public List<GameShortDto> getGameList() {
        return gameService.getGameShortList();
    }

    @Override
    public GameDto getGameInfo(Long gameId) {
        return null;
    }

    @Override
    public GameDto updateGameInfo(GameDto gameDto) {
        return null;
    }

    @Override
    public List<String> getReleaseDates() {
        return null;
    }

    @Override
    public List<String> getGamesPlatforms() {
        return null;
    }

    @Override
    public List<String> getGameGenres() {
        return null;
    }
}

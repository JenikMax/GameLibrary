package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.converter.GameConverter;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.downloads.api.DownloadService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import com.jenikmax.game.library.service.scraper.ScraperFactory;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryOperationService implements LibraryService {

    private final String rootDirectory;
    private final static String LIBRARY_PREFIX = "/games";

    private final GameService gameService;
    private final ScanerService scanerService;
    private final ScraperFactory scraperFactory;
    private final DownloadService downloadService;

    public LibraryOperationService(@Value("${game-library.games.directory}") String rootDirectory,
                                   GameService gameService, ScanerService scanerService, ScraperFactory scraperFactory, DownloadService downloadService) {
        this.rootDirectory = rootDirectory;
        this.gameService = gameService;
        this.scanerService = scanerService;
        this.scraperFactory = scraperFactory;
        this.downloadService = downloadService;
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
    public List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && sortField.isEmpty()) return getGameList();
        return gameService.getGameShortList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType);
    }

    @Override
    public GameDto getGameInfo(Long gameId) {
        return GameConverter.gameToDtoConverter(gameService.getGameById(gameId));
    }

    @Override
    public GameDto updateGameInfo(GameDto gameDto) {
        Game game = GameConverter.dtoToGameEntityConverter(gameDto);
        gameService.updateGame(game);
        scanerService.storeGame(game);
        return getGameInfo(gameDto.getId());
    }

    @Override
    public GameDto grabGameInfo(Long id, String source, String url) {
        if(url != null) return scraperFactory.getScraper(source).scrap(getGameInfo(id),url);
        return scraperFactory.getScraper(source).scrap(getGameInfo(id));
    }

    @Override
    public GameDto grabGameInfo(GameDto gameDto, String source) {
        return scraperFactory.getScraper(source).scrap(gameDto);
    }

    @Override
    public List<String> getReleaseDates() {
        return gameService.getReleaseDates();
    }

    @Override
    public List<String> getGamesPlatforms() {
        return gameService.getGamesPlatforms();
    }

    @Override
    public List<Genre> getGenres() {
        return gameService.getGenres();
    }

    @Override
    public List<Genre> getGenres(GameDto gameDto) {
        List<Genre> genres = new ArrayList<>();
        for(String genre : gameDto.getGenres()) genres.add(Genre.valueOf(genre));
        return genres;
    }
    @Override
    public List<String> getGameGenres() {
        return gameService.getGameGenres();
    }

    @Override
    public ResponseEntity<Resource> downloadGame(GameDto game) {
        ByteArrayResource resource;
        String name = game.getName();
        if(downloadService.getDirectorySizeRecursively(game.getDirectoryPath()) > 1000000000L){
            resource = downloadService.downloadTorrent(game.getDirectoryPath());
            name += ".torrent";
        }
        else {
            resource = downloadService.downloadZip(game.getDirectoryPath());
            name += ".zip";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(name).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<Resource>((Resource) resource, headers, HttpStatus.OK);
    }
}

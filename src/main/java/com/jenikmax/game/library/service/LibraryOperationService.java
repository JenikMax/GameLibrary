package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.converter.GameConverter;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.downloads.api.DownloadService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import com.jenikmax.game.library.service.scraper.ScraperFactory;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class LibraryOperationService implements LibraryService {

    private final String rootDirectory;
    private final static String LIBRARY_PREFIX = "/games";

    private final GameService gameService;
    private final UserService userService;
    private final ScanerService scanerService;
    private final ScraperFactory scraperFactory;
    private final DownloadService downloadService;

    public LibraryOperationService(@Value("${game-library.games.directory}") String rootDirectory,
                                   GameService gameService, UserService userService, ScanerService scanerService, ScraperFactory scraperFactory, DownloadService downloadService) {
        this.rootDirectory = rootDirectory;
        this.gameService = gameService;
        this.userService = userService;
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
                findGames.stream()
                        .filter(newGame -> storedGames.stream()
                                .noneMatch(storedGame ->
                                        storedGame.getDirectoryPath().equals(newGame.getDirectoryPath())))
                        .collect(Collectors.toList());
        List<Game> gamesToDelete = storedGames.stream()
                .filter(storedGame -> findGames.stream()
                                .noneMatch(findGame -> findGame.getDirectoryPath().equals(storedGame.getDirectoryPath())))
                .collect(Collectors.toList());
        for(Game gameShort : newGames){
            Game game = scanerService.getAdditinalGameInfo(gameShort);
            gameService.storeGame(game);
        }
        for(Game gameShort : gamesToDelete){
            gameService.deleteGameInfo(gameShort.getId());
        }

    }

    @Override
    public List<GameReadDto> getGameList() {
        return gameService.getGameShortList();
    }

    @Override
    public List<GameReadDto> getGameList(int startIndex, int endIndex) {
        return gameService.getGameShortList(startIndex, endIndex);
    }

    @Override
    public List<Long> getGameListId() {
        return gameService.getGameShortIdList();
    }

    @Override
    public List<GameReadDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && sortField.isEmpty()) return getGameList();
        return gameService.getGameShortList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType);
    }

    @Override
    public List<GameReadDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType, int startIndex, int endIndex) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && sortField.isEmpty()) return getGameList(startIndex,endIndex);
        return gameService.getGameShortList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType,startIndex,endIndex);
    }

    @Override
    public List<Long> getGameIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && sortField.isEmpty()) return getGameListId();
        return gameService.getGameShortIdList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType);
    }

    @Override
    public GameDto getGameInfo(Long gameId) {
        return GameConverter.gameToDtoConverter(gameService.getGameById(gameId));
    }

    @Override
    public GameReadDto getGameReadInfo(Long gameId){
        return GameConverter.gameToReadDtoConverter(gameService.getGameById(gameId));
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
    public GameDto grabGameInfo(Long id, ScrapInfo scrapInfo) {
        if(scrapInfo != null && scrapInfo.getUrl() != null)
            return scraperFactory.getScraper(scrapInfo.getSource()).scrap(getGameInfo(id),scrapInfo);
        return scraperFactory.getScraper(scrapInfo.getSource()).scrap(getGameInfo(id));
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

    public List<Genre> getGenres(Locale locale){
        return gameService.getGenres(locale);
    }

    @Override
    public List<Genre> getGenres(GameDto gameDto) {
        List<Genre> genres = new ArrayList<>();
        for(String genre : gameDto.getGenres()) genres.add(Genre.valueOf(genre));
        return genres;
    }

    @Override
    public List<Genre> getGenres(GameReadDto gameDto) {
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
        // TODO добавить реализацию отличную от зип для случая downloadService.getDirectorySizeRecursively(game.getDirectoryPath()) > 1000000000L
        resource = downloadService.downloadZip(game.getDirectoryPath());
        name += ".zip";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(name).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<Resource>((Resource) resource, headers, HttpStatus.OK);
    }

    @Override
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadGameInStream(GameDto game, HttpServletResponse response) {
        CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture = new CompletableFuture<>();
        String extension = "";
        Long size = downloadService.getDirectorySizeRecursively(game.getDirectoryPath());// ?> 1000000000L
        // Создайте StreamingResponseBody для передачи данных
        StreamingResponseBody streamingResponseBody = outputStream -> {
            if(size > 1000000000L){
                downloadService.downloadTorrent(game.getDirectoryPath(),outputStream,completableFuture);
            }
            else{
                downloadService.downloadZipInStream(game.getDirectoryPath(),outputStream,completableFuture);
            }
        };
        // Установите заголовки ответа
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + game.getName() + "(" + game.getReleaseDate() + ")" +  (size > 1000000000L ? ".torrent\"" : ".zip\""));

        // Запустите операцию формирования архива в отдельном потоке
        CompletableFuture.runAsync(() -> {
            try {
                streamingResponseBody.writeTo(response.getOutputStream());
                response.flushBuffer();
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
            }
        });

        return completableFuture;
    }

    @Override
    public byte[] getImageBytesById(Long id) {
        return gameService.getImageBytesById(id);
    }

    @Override
    public byte[] getPosterBytesById(Long id){
        return gameService.getPosterBytesById(id);
    }

    @Override
    public ShortUser getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.getUserInfoByName(username);
    }


}

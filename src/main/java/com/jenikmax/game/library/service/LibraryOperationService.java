package com.jenikmax.game.library.service;

import com.jenikmax.game.library.model.converter.GameConverter;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.ai.EmbeddingService;
import com.jenikmax.game.library.service.data.api.GameService;
import com.jenikmax.game.library.service.data.api.UserService;
import com.jenikmax.game.library.service.downloads.StreamingZipWriter;
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Реализация сервиса библиотеки игр.
 * Объединяет сканирование файловой системы, CRUD-операции над играми,
 * поиск/фильтрацию, скрапинг метаданных и скачивание игр (ZIP/Torrent).
 * Также управляет семантическим поиском через AI-сервис.
 */
@Service
public class LibraryOperationService implements LibraryService {

    private final String rootDirectory;
    private final static String LIBRARY_PREFIX = "/games";

    private final GameService gameService;
    private final UserService userService;
    private final ScanerService scanerService;
    private final ScraperFactory scraperFactory;
    private final DownloadService downloadService;
    private final EmbeddingService embeddingService;

    @PersistenceContext
    private EntityManager entityManager;

    public LibraryOperationService(@Value("${game-library.games.directory}") String rootDirectory,
                                   GameService gameService, UserService userService, ScanerService scanerService, ScraperFactory scraperFactory, DownloadService downloadService,
                                   EmbeddingService embeddingService) {
        this.rootDirectory = rootDirectory;
        this.gameService = gameService;
        this.userService = userService;
        this.scanerService = scanerService;
        this.scraperFactory = scraperFactory;
        this.downloadService = downloadService;
        this.embeddingService = embeddingService;
    }

    /**
     * Запускает полное сканирование директории с играми.
     * Добавляет новые игры, обновляет метаданные, загружает изображения и удаляет отсутствующие.
     */
    @Override
    public void scanLibrary() {
        List<Game> findGames = scanerService.scanDirectory(rootDirectory + LIBRARY_PREFIX);

        // лёгкий запрос — только id + directoryPath, без byte[]
        List<Object[]> storedPaths = gameService.getGameDirectoryPaths();
        Map<String, Long> storedMap = new HashMap<>();
        for (Object[] row : storedPaths) {
            storedMap.put((String) row[1], (Long) row[0]);
        }

        List<Game> newGames = storedMap.isEmpty() ?
                findGames :
                findGames.stream()
                        .filter(newGame -> !storedMap.containsKey(newGame.getDirectoryPath()))
                        .collect(Collectors.toList());

        List<Game> gamesToDelete = storedMap.isEmpty() ?
                new ArrayList<>() :
                storedMap.entrySet().stream()
                        .filter(entry -> findGames.stream()
                                .noneMatch(findGame -> findGame.getDirectoryPath().equals(entry.getKey())))
                        .map(entry -> { Game g = new Game(); g.setId(entry.getValue()); return g; })
                        .collect(Collectors.toList());

        // Проход 1: метаданные (без byte[])
        List<Long> newGameIds = new ArrayList<>();
        for (Game gameShort : newGames) {
            Game game = scanerService.getBasicGameInfo(gameShort);
            game = gameService.storeGameMetadata(game);
            newGameIds.add(game.getId());
        }

        // Проход 2: изображения (с очисткой persistence context после каждого сохранения)
        for (Long gameId : newGameIds) {
            Game game = entityManager.find(Game.class, gameId);
            byte[] logo = scanerService.getLogo(game);
            game.setLogo(logo);
            List<Screenshot> screenshots = scanerService.getScreenshots(game);
            game.getScreenshots().clear();
            game.getScreenshots().addAll(screenshots);
            gameService.updateGameImages(game);
            entityManager.clear();
        }

        for (Game gameShort : gamesToDelete) {
            gameService.deleteGameInfo(gameShort.getId());
        }

    }

    /**
     * Возвращает краткий список всех игр.
     */
    @Override
    public List<GameShortDto> getGameList() {
        return gameService.getGameShortList();
    }

    /**
     * Возвращает краткий список игр с пагинацией.
     */
    @Override
    public List<GameShortDto> getGameList(int startIndex, int endIndex) {
        return gameService.getGameShortList(startIndex, endIndex);
    }

    /**
     * Возвращает список идентификаторов всех игр.
     */
    @Override
    public List<Long> getGameListId() {
        return gameService.getGameShortIdList();
    }

    /**
     * Возвращает отфильтрованный список игр с сортировкой (без пагинации).
     */
    @Override
    public List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        return getGameList(searchText, selectedPlatforms, selectedYears, selectedGenres, null, sortField, sortType, 0, 0);
    }

    public List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType, int startIndex, int endIndex) {
        return getGameList(searchText, selectedPlatforms, selectedYears, selectedGenres, null, sortField, sortType, startIndex, endIndex);
    }

    /**
     * Возвращает отфильтрованный список игр с сортировкой, пагинацией и фильтром по тегам.
     */
    public List<GameShortDto> getGameList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, List<String> selectedTags, String sortField, String sortType, int startIndex, int endIndex) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && (selectedTags == null || selectedTags.size() == 0) && sortField.isEmpty()) return getGameList(startIndex,endIndex);
        return gameService.getGameShortList(searchText,selectedPlatforms,selectedYears,selectedGenres,selectedTags,sortField,sortType,startIndex,endIndex);
    }

    /**
     * Возвращает краткий список игр по переданному набору идентификаторов.
     */
    @Override
    public List<GameShortDto> getGameShortListByIds(List<Long> ids) {
        return gameService.getGameShortListByIds(ids);
    }

    /**
     * Возвращает отфильтрованный список ID игр (без пагинации).
     */
    public List<Long> getGameIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        return getGameIdList(searchText, selectedPlatforms, selectedYears, selectedGenres, null, sortField, sortType);
    }

    /**
     * Возвращает отфильтрованный список ID игр с фильтром по тегам.
     */
    @Override
    public List<Long> getGameIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, List<String> selectedTags, String sortField, String sortType) {
        if(searchText.isEmpty() && selectedPlatforms.size() == 0 && selectedYears.size() == 0 && selectedGenres.size() == 0 && (selectedTags == null || selectedTags.size() == 0) && sortField.isEmpty()) return getGameListId();
        return gameService.getGameShortIdList(searchText,selectedPlatforms,selectedYears,selectedGenres,selectedTags,sortField,sortType);
    }

    /**
     * Выполняет семантический поиск игр по текстовому запросу через AI-эмбеддинги.
     */
    @Override
    public List<Long> semanticSearchGameIds(String query, int limit) {
        return gameService.semanticSearchGameIds(query, limit);
    }

    /**
     * Фильтрует список ID игр по платформам, годам, жанрам и тегам.
     */
    @Override
    public List<Long> filterGameIdsByCriteria(List<Long> candidateIds, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, List<String> selectedTags) {
        return gameService.filterGameIdsByCriteria(candidateIds, selectedPlatforms, selectedYears, selectedGenres, selectedTags);
    }

    /**
     * Проверяет доступность семантического поиска (модель + хотя бы один эмбеддинг).
     */
    @Override
    public boolean isSemanticSearchAvailable() {
        return gameService.isEmbeddingModelAvailable() && gameService.hasEmbeddings();
    }

    /**
     * Возвращает список всех тегов.
     */
    @Override
    public List<String> getTags() {
        return gameService.getTags();
    }

    /**
     * Возвращает полную информацию об игре по ID, включая переведённое описание.
     */
    @Override
    public GameDto getGameInfo(Long gameId) {
        GameDto dto = GameConverter.gameToDtoConverter(gameService.getGameById(gameId));
        dto.setDescriptionTranslated(gameService.getDescriptionTranslated(gameId));
        return dto;
    }

    /**
     * Обновляет информацию об игре: сохраняет данные, сбрасывает перевод,
     * сохраняет на ФС и генерирует эмбеддинг.
     */
    @Override
    public GameDto updateGameInfo(GameDto gameDto) {
        gameService.ensureTagsExist(gameDto.getTags());
        Game game = GameConverter.dtoToGameEntityConverter(gameDto);
        gameService.updateGame(game);
        gameService.resetDescriptionTranslated(game.getId());
        scanerService.storeGame(game);
        embeddingService.generateAndStore(game.getId());
        return getGameInfo(gameDto.getId());
    }

    /**
     * Скрапит метаданные игры из указанного источника по URL.
     */
    @Override
    public GameDto grabGameInfo(Long id, String source, String url) {
        GameDto gameDto = new GameDto();
        gameDto.setId(id);
        if(url != null && !url.isEmpty()) return scraperFactory.getScraper(source).scrap(gameDto, url);
        Game game = gameService.getGameById(id);
        if (game != null) gameDto.setName(game.getName());
        return scraperFactory.getScraper(source).scrap(gameDto);
    }

    /**
     * Скрапит метаданные игры с использованием структурированной информации ScrapInfo.
     */
    @Override
    public GameDto grabGameInfo(Long id, ScrapInfo scrapInfo) {
        GameDto gameDto = new GameDto();
        gameDto.setId(id);
        if(scrapInfo != null && scrapInfo.getUrl() != null && !scrapInfo.getUrl().isEmpty())
            return scraperFactory.getScraper(scrapInfo.getSource()).scrap(gameDto, scrapInfo);
        Game game = gameService.getGameById(id);
        if (game != null) gameDto.setName(game.getName());
        return scraperFactory.getScraper(scrapInfo.getSource()).scrap(gameDto);
    }

    /**
     * Скрапит метаданные для DTO игры из указанного источника.
     */
    @Override
    public GameDto grabGameInfo(GameDto gameDto, String source) {
        return scraperFactory.getScraper(source).scrap(gameDto);
    }

    /**
     * Возвращает список всех годов релиза.
     */
    @Override
    public List<String> getReleaseDates() {
        return gameService.getReleaseDates();
    }

    /**
     * Возвращает список всех платформ.
     */
    @Override
    public List<String> getGamesPlatforms() {
        return gameService.getGamesPlatforms();
    }

    /**
     * Возвращает список всех жанров.
     */
    @Override
    public List<Genre> getGenres() {
        return gameService.getGenres();
    }

    /**
     * Возвращает список жанров с учётом локали.
     */
    public List<Genre> getGenres(Locale locale){
        return gameService.getGenres(locale);
    }

    /**
     * Преобразует строковые жанры DTO в enum Genre.
     */
    @Override
    public List<Genre> getGenres(GameDto gameDto) {
        List<Genre> genres = new ArrayList<>();
        for(String genre : gameDto.getGenres()) genres.add(Genre.valueOf(genre));
        return genres;
    }
    /**
     * Возвращает список кодов жанров (строки).
     */
    @Override
    public List<String> getGameGenres() {
        return gameService.getGameGenres();
    }

    /**
     * Скачивает игру как ZIP-архив (для небольших игр до 5 ГБ).
     */
    @Override
    public ResponseEntity<Resource> downloadGame(GameDto game) {
        ByteArrayResource resource;
        String name = game.getName();
        // TODO добавить реализацию отличную от зип для случая downloadService.getDirectorySizeRecursively(game.getDirectoryPath()) > 5L * 1024 * 1024 * 1024
        resource = downloadService.downloadZip(game.getDirectoryPath());
        name += ".zip";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(name).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        return new ResponseEntity<Resource>((Resource) resource, headers, HttpStatus.OK);
    }

    /**
     * Скачивает игру стримингом: ZIP для игр до 5 ГБ, .torrent для больших игр.
     */
    @Override
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadGameInStream(GameDto game, HttpServletResponse response) {
        CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture = new CompletableFuture<>();
        Long size = downloadService.getDirectorySizeRecursively(game.getDirectoryPath());

        if (size > 5L * 1024 * 1024 * 1024) {
            long torrentSize;
            try {
                torrentSize = downloadService.getCachedTorrentSize(game.getDirectoryPath());
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return completableFuture;
            }
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(torrentSize));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + game.getName() + "(" + game.getReleaseDate() + ").torrent\"");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            StreamingResponseBody streamingResponseBody = outputStream -> {
                downloadService.serveCachedTorrent(game.getDirectoryPath(), outputStream, completableFuture);
            };

            CompletableFuture.runAsync(() -> {
                try {
                    streamingResponseBody.writeTo(response.getOutputStream());
                    response.flushBuffer();
                } catch (IOException e) {
                    completableFuture.completeExceptionally(e);
                }
            });
        } else {
            StreamingZipWriter.ZipManifest manifest;
            try {
                manifest = downloadService.buildZipManifest(game.getDirectoryPath());
            } catch (IOException e) {
                completableFuture.completeExceptionally(e);
                return completableFuture;
            }
            response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(manifest.zipSize));
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + game.getName() + "(" + game.getReleaseDate() + ").zip\"");
            response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);

            StreamingResponseBody streamingResponseBody = outputStream -> {
                downloadService.downloadZipWithManifest(game.getDirectoryPath(), outputStream, manifest, completableFuture);
            };

            CompletableFuture.runAsync(() -> {
                try {
                    streamingResponseBody.writeTo(response.getOutputStream());
                    response.flushBuffer();
                } catch (IOException e) {
                    completableFuture.completeExceptionally(e);
                }
            });
        }

        return completableFuture;
    }

    /**
     * Возвращает случайную игру из библиотеки.
     */
    @Override
    public GameDto getRandomGame() {
        Long gameId = gameService.findRandomGameId();
        if (gameId == null) return null;
        return getGameInfo(gameId);
    }

    /**
     * Возвращает информацию о текущем аутентифицированном пользователе.
     */
    @Override
    public ShortUser getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userService.getUserInfoByName(username);
    }


}

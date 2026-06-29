package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.ScreenshotRepository;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.api.*;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.scraper.ScraperConfigService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/games")
public class LibraryController {

    private static final Logger logger = LoggerFactory.getLogger(LibraryController.class);

    private final LibraryService libraryService;
    private final ScreenshotRepository screenshotRepository;
    private final ScraperConfigService scraperConfigService;
    private final String imagesDirectory;
    private final MessageSource messageSource;

    public LibraryController(LibraryService libraryService,
                             ScreenshotRepository screenshotRepository,
                             ScraperConfigService scraperConfigService,
                             MessageSource messageSource,
                             @Value("${game-library.images.directory:/gameLibrary/images}") String imagesDirectory) {
        this.libraryService = libraryService;
        this.screenshotRepository = screenshotRepository;
        this.scraperConfigService = scraperConfigService;
        this.messageSource = messageSource;
        this.imagesDirectory = imagesDirectory;
    }

    @GetMapping("/scrapers")
    public ResponseEntity<ApiResponse<List<ScraperInfoResponse>>> getScraperSources() {
        List<ScraperInfoResponse> items = scraperConfigService.getEnabledConfigs().stream()
                .map(c -> new ScraperInfoResponse(c.getType(), c.getDisplayName()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<GameListResponse>>> getGames(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "search", required = false) String searchText,
            @RequestParam(value = "platforms", required = false) List<String> selectedPlatforms,
            @RequestParam(value = "years", required = false) List<String> selectedYears,
            @RequestParam(value = "genres", required = false) List<String> selectedGenres,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortType", required = false) String sortType,
            Locale locale) {

        searchText = searchText != null ? searchText : "";
        selectedPlatforms = selectedPlatforms != null ? selectedPlatforms : new ArrayList<>();
        selectedYears = selectedYears != null ? selectedYears : new ArrayList<>();
        selectedGenres = selectedGenres != null ? selectedGenres : new ArrayList<>();
        sortField = sortField != null ? sortField : "";
        sortType = sortType != null ? sortType : "";

        List<Long> gameIdList = libraryService.getGameIdList(searchText, selectedPlatforms, selectedYears, selectedGenres, sortField, sortType);

        int pageSize = 12;
        int totalPages = (gameIdList.size() + pageSize - 1) / pageSize;
        totalPages = totalPages == 0 ? 1 : totalPages;
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, gameIdList.size());

        List<GameShortDto> paginatedGames = libraryService.getGameList(searchText, selectedPlatforms, selectedYears, selectedGenres, sortField, sortType, startIndex, endIndex);

        List<GameListResponse> items = paginatedGames.stream()
                .map(this::toGameListResponse)
                .collect(Collectors.toList());

        PageResponse<GameListResponse> pageResponse = new PageResponse<>(
                items, page, totalPages, gameIdList.size(), pageSize);

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<FilterOptionsResponse>> getFilterOptions(Locale locale) {
        List<String> years = libraryService.getReleaseDates();
        List<String> platforms = libraryService.getGamesPlatforms();
        List<Genre> genres = libraryService.getGenres(locale);

        FilterOptionsResponse options = new FilterOptionsResponse();
        options.setYears(years);
        options.setPlatforms(platforms);
        options.setGenres(genres.stream()
                .map(g -> new FilterOptionsResponse.GenreItem(g.name(),
                        messageSource.getMessage("enum.genre." + g.name(), null, g.getName(), locale)))
                .collect(Collectors.toList()));

        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GameDetailResponse>> getGame(@PathVariable Long id) {
        logger.info("REST get game - {}", id);
        GameDto gameDto = libraryService.getGameInfo(id);
        return ResponseEntity.ok(ApiResponse.ok(toGameDetailResponse(gameDto)));
    }

    @PostMapping("/{id}/edit")
    public ResponseEntity<ApiResponse<GameDetailResponse>> editGame(@PathVariable Long id, @RequestBody GameEditRequest gameEdit) {
        logger.info("REST edit game - {}", id);
        try {
            // Delete screenshots by IDs before updating
            if (gameEdit.getDeleteScreenshotIds() != null && !gameEdit.getDeleteScreenshotIds().isEmpty()) {
                screenshotRepository.deleteAllById(gameEdit.getDeleteScreenshotIds());
                screenshotRepository.flush();
            }

            // Load existing game DTO to preserve logo & screenshots not being deleted
            GameDto existing = libraryService.getGameInfo(id);

            GameDto gameDto = new GameDto();
            gameDto.setId(id);
            gameDto.setCreateTs(existing.getCreateTs());
            gameDto.setName(gameEdit.getName());
            gameDto.setPlatform(gameEdit.getPlatform());
            gameDto.setReleaseDate(gameEdit.getReleaseDate());
            gameDto.setDescription(gameEdit.getDescription());
            gameDto.setInstruction(gameEdit.getInstruction());
            gameDto.setTrailerUrl(gameEdit.getTrailerUrl());
            gameDto.setGenres(gameEdit.getGenres() != null ? gameEdit.getGenres() : new ArrayList<>());
            gameDto.setDirectoryPath(gameEdit.getDirectoryPath());

            // Handle logo: use new one if provided, else keep existing
            if (gameEdit.getLogo() != null && !gameEdit.getLogo().isEmpty()) {
                gameDto.setLogo(gameEdit.getLogo());
            } else {
                gameDto.setLogo(existing.getLogo());
            }

            // Use form screenshots as complete set, or keep existing if none provided
            if (gameEdit.getScreenshots() != null && !gameEdit.getScreenshots().isEmpty()) {
                gameDto.setScreenshots(gameEdit.getScreenshots());
            } else {
                gameDto.setScreenshots(existing.getScreenshots());
            }

            GameDto updated = libraryService.updateGameInfo(gameDto);
            return ResponseEntity.ok(ApiResponse.ok(toGameDetailResponse(updated)));
        } catch (Exception e) {
            logger.error("Edit game error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update game: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/grab")
    public ResponseEntity<ApiResponse<GameDetailResponse>> grabGameData(
            @PathVariable Long id,
            @RequestBody ScrapRequest scrapRequest) {
        logger.info("REST grab game data - {}, source - {}", id, scrapRequest.getSource());
        try {
            ScrapInfo scrapInfo = new ScrapInfo(
                    scrapRequest.getUrl(),
                    scrapRequest.getSource(),
                    scrapRequest.isTitle(),
                    scrapRequest.isPoster(),
                    scrapRequest.isDescription(),
                    scrapRequest.isYear(),
                    scrapRequest.isGenres(),
                    scrapRequest.isScreens());
            GameDto gameDto = libraryService.grabGameInfo(id, scrapInfo);
            return ResponseEntity.ok(ApiResponse.ok(toGameDetailResponse(gameDto)));
        } catch (Exception e) {
            logger.error("Grab game data error", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to grab data: " + e.getMessage()));
        }
    }

    private GameListResponse toGameListResponse(GameShortDto dto) {
        GameListResponse resp = new GameListResponse();
        resp.setId(dto.getId());
        resp.setName(dto.getName());
        resp.setPlatform(dto.getPlatform());
        resp.setReleaseDate(dto.getReleaseDate());
        resp.setGenres(dto.getGenres());
        resp.setLogoUrl(buildLogoUrl(dto.getId()));
        resp.setLogo(dto.getLogo());
        return resp;
    }

    private GameDetailResponse toGameDetailResponse(GameDto dto) {
        GameDetailResponse resp = new GameDetailResponse();
        resp.setId(dto.getId());
        resp.setName(dto.getName());
        resp.setPlatform(dto.getPlatform());
        resp.setReleaseDate(dto.getReleaseDate());
        resp.setDirectoryPath(dto.getDirectoryPath());
        resp.setGenres(dto.getGenres());
        resp.setLogoUrl(buildLogoUrl(dto.getId()));
        resp.setTrailerUrl(dto.getTrailerUrl());
        resp.setDescription(dto.getDescription());
        resp.setInstruction(dto.getInstruction());

        // Передаём base64 данные — нужны фронтенду для формы редактирования и grab-ответа
        resp.setLogo(dto.getLogo());
        resp.setScreenshots(dto.getScreenshots());

        List<String> screenshotUrls = new ArrayList<>();
        try {
            List<Long> screenshotIds = screenshotRepository.findIdsByGameId(dto.getId());
            for (Long screenshotId : screenshotIds) {
                screenshotUrls.add("/game-library/api/images/games/" + dto.getId() + "/screenshots/" + screenshotId);
            }
        } catch (Exception e) {
            logger.warn("Could not load screenshots for game {}", dto.getId());
        }
        resp.setScreenshotUrls(screenshotUrls);

        return resp;
    }

    private String buildLogoUrl(Long gameId) {
        String baseUrl = "/game-library/api/images/games/" + gameId + "/logo";
        try {
            Path logoPath = Paths.get(imagesDirectory, "games", String.valueOf(gameId), "logo.jpg");
            if (Files.exists(logoPath)) {
                long modified = Files.getLastModifiedTime(logoPath).toMillis();
                return baseUrl + "?v=" + modified;
            }
        } catch (IOException e) {
            // ignore, return base URL
        }
        return baseUrl;
    }
}

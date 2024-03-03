package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.ShortUser;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.scraper.api.ScrapInfo;
import com.jenikmax.game.library.service.utils.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.util.NumberUtils;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Controller
public class LibraryViewController {

    static final Logger logger = LogManager.getLogger(LibraryViewController.class.getName());

    private final LibraryService libraryService;
    private final MessageSource messageSource;
    private final StringUtils stringUtils;

    public LibraryViewController(LibraryService libraryService, MessageSource messageSource, StringUtils stringUtils) {
        this.libraryService = libraryService;
        this.messageSource = messageSource;
        this.stringUtils = stringUtils;
    }

    @GetMapping("/library")
    public String main(Model model, Locale locale, @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "searchText", required = false) String searchText,
                        @RequestParam(value = "message", required = false) String message) {
        logger.info("Open library");
        searchText = searchText != null ? searchText : "";
        message = message != null ? message : "";
        ArrayList<String> selectedPlatforms = new ArrayList<>();
        ArrayList<String> selectedYears = new ArrayList<>();
        ArrayList<String> selectedGenres = new ArrayList<>();
        String sortField = "";
        String sortType = "";

        int pageSize = 12;
        int totalPages = 1;

        List<GameShortDto> paginatedGames = new ArrayList<>();

        List<String> years = libraryService.getReleaseDates();
        List<String> platforms = libraryService.getGamesPlatforms();
        List<Genre> genres = libraryService.getGenres(locale);
        ShortUser user = libraryService.getUserInfo();

        model.addAttribute("searchText", searchText);
        model.addAttribute("selectedPlatforms", selectedPlatforms);
        model.addAttribute("selectedYears", selectedYears);
        model.addAttribute("selectedGenres", selectedGenres);
        model.addAttribute("gameList",paginatedGames);
        model.addAttribute("years", years);
        model.addAttribute("platforms", platforms);
        model.addAttribute("genres", genres);
        model.addAttribute("page", page);
        model.addAttribute("pages", initPages(page,totalPages));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("message", message);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortType", sortType);
        model.addAttribute("user", user);

        model.addAttribute("messageSource", messageSource);
        model.addAttribute("locale", locale);

        return "libraryView";
    }



    @PostMapping("/scan")
    public String scanLibrary(RedirectAttributes redirectAttributes,Locale locale) {
        logger.info("Scan library");
        libraryService.scanLibrary();
        redirectAttributes.addAttribute("message",messageSource.getMessage("view.library.scan.message",null,locale));
        return "redirect:/library";
    }


    @PostMapping("/search")
    public String search(Model model, RedirectAttributes redirectAttributes,
                               @RequestParam(value = "searchText", required = false) String searchText) {
        redirectAttributes.addAttribute("searchText",searchText);
        return "redirect:/library";
    }



    @GetMapping("/library-games")
    public String applySortGet(Model model,
                            @RequestParam(value = "page", defaultValue = "1") int page,
                            @RequestParam(value = "searchText", required = false) String searchText,
                            @RequestParam(value = "selectedPlatforms", required = false) List<String> selectedPlatforms,
                            @RequestParam(value = "selectedYears", required = false) List<String> selectedYears,
                            @RequestParam(value = "selectedGenres", required = false) List<String> selectedGenres,
                            @RequestParam(value = "sortField", required = false) String sortField,
                            @RequestParam(value = "sortType", required = false) String sortType) {

        searchText = searchText != null ? searchText : "";
        selectedPlatforms = selectedPlatforms != null ? selectedPlatforms : new ArrayList<>();
        selectedYears = selectedYears != null ? selectedYears : new ArrayList<>();
        selectedGenres = selectedGenres != null ? selectedGenres : new ArrayList<>();
        sortField = sortField != null ? sortField : "";
        sortType = sortType != null ? sortType : "";

        List<Long> gameIdList = libraryService.getGameIdList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType);

        int pageSize = 12;
        int totalPages = (gameIdList.size() + pageSize - 1) / pageSize;
        totalPages = totalPages == 0 ? 1 : totalPages;
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min((startIndex + pageSize), gameIdList.size());

        List<GameReadDto> paginatedGames = libraryService.getGameList(searchText,selectedPlatforms,selectedYears,selectedGenres,sortField,sortType, startIndex, endIndex);

        model.addAttribute("gameList",paginatedGames);
        model.addAttribute("page", page);
        model.addAttribute("pages", initPages(page,totalPages));
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortType", sortType);


        return "libraryList";
    }
    



    @GetMapping("library/game/{id}")
    public String viewGame(@PathVariable("id") Long id, Model model,Locale locale) {
        logger.info("Open game - {}",id);
        GameReadDto gameDto = libraryService.getGameReadInfo(id);
        List<Genre> currentGenres = libraryService.getGenres(gameDto);
        ShortUser user = libraryService.getUserInfo();
        model.addAttribute("game", gameDto);
        model.addAttribute("current_genres", currentGenres);
        model.addAttribute("user", user);

        model.addAttribute("messageSource", messageSource);
        model.addAttribute("stringUtils", stringUtils);
        model.addAttribute("locale", locale);

        return "gameView";
    }

    @GetMapping("library/game/{id}/edit")
    public String editGame(@PathVariable("id") Long id, Model model, Locale locale) {
        logger.info("Open game - {}",id);
        GameDto gameDto = libraryService.getGameInfo(id);
        List<Genre> genres = libraryService.getGenres(locale);
        List<Genre> currentGenres = libraryService.getGenres(gameDto);
        ShortUser user = libraryService.getUserInfo();
        model.addAttribute("game", gameDto);
        model.addAttribute("genres", genres);
        model.addAttribute("current_genres", currentGenres);
        model.addAttribute("user", user);

        model.addAttribute("messageSource", messageSource);
        model.addAttribute("locale", locale);

        return "gameEditView";
    }

    @PostMapping("library/game/{id}/edit")
    public String saveGame(@PathVariable("id") Long id, GameDto game,RedirectAttributes redirectAttributes, Model model, Locale locale) {
        logger.info("Edit game - {} ",id);
        try{
            libraryService.updateGameInfo(game);
            redirectAttributes.addAttribute("message",messageSource.getMessage("view.game.save.message",null,locale));
        }
        catch (Exception e){
            logger.error("SaveGame Error - ",e);
            redirectAttributes.addAttribute("message",messageSource.getMessage("view.system.error",null,locale));
        }
        return "redirect:/library";
    }

    @PostMapping("library/game/{id}/grab")
    public String grabGameData(@PathVariable("id") Long id,
                               @RequestParam(value = "source") String source,
                               @RequestParam(value = "url") String url,
                               @RequestParam(value = "title", required = false, defaultValue = "false") Boolean titleAttr,
                               @RequestParam(value = "poster", required = false, defaultValue = "false") Boolean posterAttr,
                               @RequestParam(value = "description", required = false, defaultValue = "false") Boolean descriptionAttr,
                               @RequestParam(value = "year", required = false, defaultValue = "false") Boolean yearAttr,
                               @RequestParam(value = "genres", required = false, defaultValue = "false") Boolean genresAttr,
                               @RequestParam(value = "screens", required = false, defaultValue = "false") Boolean screensAttr,
                               Model model, Locale locale) {
        logger.info("Grab game data game - {}, source - {}, url - {}",id,source,url);

        ScrapInfo scrapInfo = new ScrapInfo(url,source,titleAttr,posterAttr,descriptionAttr,yearAttr,genresAttr,screensAttr);
        GameDto gameDto = libraryService.grabGameInfo(id,scrapInfo);
        List<Genre> genres = libraryService.getGenres(locale);
        List<Genre> currentGenres = libraryService.getGenres(gameDto);
        ShortUser user = libraryService.getUserInfo();
        model.addAttribute("game", gameDto);
        model.addAttribute("genres", genres);
        model.addAttribute("current_genres", currentGenres);
        model.addAttribute("user", user);

        model.addAttribute("messageSource", messageSource);
        model.addAttribute("locale", locale);

        return "gameEditView";
    }

    @GetMapping("library/game/{id}/download")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadGame(@PathVariable("id") Long id, HttpServletResponse response) {
        logger.info("Download game - {}",id);
        GameDto gameDto = libraryService.getGameInfo(id);
        return libraryService.downloadGameInStream(gameDto,response);

    }

    private List<Integer> initPages(int page, int totalPage){
        return Arrays.asList(NumberUtils.sequence(Math.max(1, page - 3),Math.min(totalPage, page + 3)));
    }

    @GetMapping("library/image/{id}")
    public ResponseEntity<byte[]> getImage(@PathVariable("id") Long id) {
        byte[] imageBytes = libraryService.getImageBytesById(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    @GetMapping("library/poster/{id}")
    public ResponseEntity<byte[]> getPoster(@PathVariable("id") Long id) {
        byte[] imageBytes = libraryService.getPosterBytesById(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }


}

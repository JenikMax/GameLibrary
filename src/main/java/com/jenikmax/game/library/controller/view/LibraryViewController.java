package com.jenikmax.game.library.controller.view;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.expression.Numbers;
import org.thymeleaf.util.NumberUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class LibraryViewController {

    static final Logger logger = LogManager.getLogger(LibraryViewController.class.getName());

    private final LibraryService libraryService;

    public LibraryViewController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/library")
    public String main(Model model, @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "searchText", required = false) String searchText,
                       @RequestParam(value = "selectedPlatforms", required = false) List<String> selectedPlatforms,
                       @RequestParam(value = "selectedYears", required = false) List<String> selectedYears,
                       @RequestParam(value = "selectedGenres", required = false) List<String> selectedGenres) {
        logger.info("Open library");

        searchText = searchText != null ? searchText : "";
        selectedPlatforms = selectedPlatforms != null ? selectedPlatforms : new ArrayList<>();
        selectedYears = selectedYears != null ? selectedYears : new ArrayList<>();
        selectedGenres = selectedGenres != null ? selectedGenres : new ArrayList<>();



        List<GameShortDto> gameList = libraryService.getGameList(searchText,selectedPlatforms,selectedYears,selectedGenres);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        //gameList.addAll(gameList);
        int pageSize = 12;
        int totalPages = (gameList.size() + pageSize - 1) / pageSize;
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min((startIndex + pageSize), gameList.size());

        List<GameShortDto> paginatedGames = gameList.subList(startIndex, endIndex);

        List<String> years = libraryService.getReleaseDates();
        List<String> platforms = libraryService.getGamesPlatforms();
        List<String> genres = libraryService.getGameGenres();





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
        return "libraryView";
    }



    @PostMapping("/scan")
    public String scanLibrary(Model model) {
        logger.info("Scan library");
        libraryService.scanLibrary();
        model.addAttribute("message","scan library in progress");
        return "redirect:/library";
    }


    @PostMapping("/filter")
    public String applyFilters(Model model, RedirectAttributes redirectAttributes,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @RequestParam(value = "searchText", required = false) String searchText,
                               @RequestParam(value = "selectedPlatforms", required = false) List<String> selectedPlatforms,
                               @RequestParam(value = "selectedYears", required = false) List<String> selectedYears,
                               @RequestParam(value = "selectedGenres", required = false) List<String> selectedGenres) {
        redirectAttributes.addAttribute("page",page);
        redirectAttributes.addAttribute("searchText",searchText);
        redirectAttributes.addAttribute("selectedPlatforms",selectedPlatforms != null ? selectedPlatforms : new ArrayList<>());
        redirectAttributes.addAttribute("selectedYears",selectedYears != null ? selectedYears : new ArrayList<>());
        redirectAttributes.addAttribute("selectedGenres",selectedGenres != null ? selectedGenres : new ArrayList<>());
        return "redirect:/library";
    }


    @GetMapping("library/game/{id}")
    public String viewGame(@PathVariable("id") Long id, Model model) {
        logger.info("Open game - {}",id);
        GameDto gameDto = libraryService.getGameInfo(id);
        model.addAttribute("game", gameDto);
        return "gameView";
    }

    @GetMapping("library/game/{id}/edit")
    public String editGame(@PathVariable("id") Long id, Model model) {
        logger.info("Open game - {}",id);
        GameDto gameDto = libraryService.getGameInfo(id);
        model.addAttribute("game", gameDto);
        return "gameEditView";
    }

    private List<Integer> initPages(int page, int totalPage){
        return Arrays.asList(NumberUtils.sequence(Math.max(1, page - 3),Math.min(totalPage, page + 3)));
    }







    // ${#numbers.sequence(Math.max(1, page - 3), Math.min(totalPages, page + 3))}"

}

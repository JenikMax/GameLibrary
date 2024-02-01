package com.jenikmax.game.library.controller.view;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class TestViewController {

    static final Logger logger = LogManager.getLogger(TestViewController.class.getName());

    @GetMapping("/message")
    public String displayMessage(Model model) {
        logger.info("Start displayMessage!!!");
        model.addAttribute("message", "Hello, World!");
        return "messageView";
    }

    private List<Game> gameList = new ArrayList<>(Arrays.asList(
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg"),
            new Game("Игра 1", "Платформа 1", "2000", "https://example.com/game1.jpg"),
            new Game("Игра 2", "Платформа 2", "2015", "https://example.com/game2.jpg"),
            new Game("Игра 3", "Платформа 1", "2010", "https://example.com/game3.jpg")
    ));

    private List<String> selectedPlatforms = new ArrayList<>();
    private List<String> selectedYears = new ArrayList<>();
    private List<String> selectedGenres = new ArrayList<>();

    @GetMapping("/test")
    public String gameList(Model model,
                           @RequestParam(value = "page", defaultValue = "1") int page,
                           @RequestParam(value = "searchText", required = false) String searchText) {

        List<Game> filteredGames = filterGames(searchText);
        int pageSize = 6;
        int totalPages = (filteredGames.size() + pageSize - 1) / pageSize;
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min((startIndex + pageSize), filteredGames.size());

        List<Game> paginatedGames = filteredGames.subList(startIndex, endIndex);

        List<String> years = getAllYears();
        List<String> platforms = getAllPlatforms();
        List<String> genres = getAllGenres();

        model.addAttribute("games", paginatedGames);
        model.addAttribute("page", page);
        model.addAttribute("searchText", searchText);
        model.addAttribute("selectedPlatforms", selectedPlatforms);
        model.addAttribute("selectedYears", selectedYears);
        model.addAttribute("selectedGenres", selectedGenres);
        model.addAttribute("years", years);
        model.addAttribute("platforms", platforms);
        model.addAttribute("genres", genres);
        model.addAttribute("totalPages", totalPages);

        return "testView";
    }

    // Фильтрация игр по параметрам
    private List<Game> filterGames(String searchText) {
        List<Game> filteredGames = new ArrayList<>();

        for (Game game : gameList) {
            if ((selectedYears.isEmpty() || selectedYears.contains(game.getReleaseYear()))
                    && (selectedPlatforms.isEmpty() || selectedPlatforms.contains(game.getPlatform()))
                    && (selectedGenres.isEmpty() || selectedGenres.contains(game.getGenre()))
                    && (searchText == null || game.getName().toLowerCase().contains(searchText.toLowerCase()))) {
                filteredGames.add(game);
            }
        }

        return filteredGames;
    }

    // Получение всех годов выпуска игр
    private List<String> getAllYears() {
        List<String> years = new ArrayList<>();

        for (Game game : gameList) {
            if (!years.contains(game.getReleaseYear())) {
                years.add(game.getReleaseYear());
            }
        }

        years.sort(String::compareTo);
        return years;
    }

    // Получение всех платформ
    private List<String> getAllPlatforms() {
        List<String> platforms = new ArrayList<>();

        for (Game game : gameList) {
            if (!platforms.contains(game.getPlatform())) {
                platforms.add(game.getPlatform());
            }
        }

        platforms.sort(String::compareTo);
        return platforms;
    }

    // Получение всех жанров
    private List<String> getAllGenres() {
    List<String> genres = new ArrayList<>(Arrays.asList("Жанр 1", "Жанр 2", "Жанр 3"));
    return genres;
}

    @PostMapping("/search")
    public String searchGames(Model model, @RequestParam("searchText") String searchText) {
        clearFilters();
        return "redirect:/test?searchText=" + searchText;
    }

    @PostMapping("/refresh")
    public String refreshGames(Model model) {
        clearFilters();
        // Обновление списка игр

        return "redirect:/test";
    }

    @PostMapping("/filter")
    public String applyFilters(Model model,
                               @RequestParam(value = "selectedPlatforms", required = false) List<String> selectedPlatforms,
                               @RequestParam(value = "selectedYears", required = false) List<String> selectedYears,
                               @RequestParam(value = "selectedGenres", required = false) List<String> selectedGenres) {
        this.selectedPlatforms = selectedPlatforms != null ? selectedPlatforms : new ArrayList<>();
        this.selectedYears = selectedYears != null ? selectedYears : new ArrayList<>();
        this.selectedGenres = selectedGenres != null ? selectedGenres : new ArrayList<>();

        return "redirect:/test";
    }

    private void clearFilters() {
        selectedPlatforms.clear();
        selectedYears.clear();
        selectedGenres.clear();
    }

    public class Game {

        private String name;
        private String platform;
        private String releaseYear;
        private String imageUrl;

        public Game() {
        }

        public Game(String name, String platform, String releaseYear, String imageUrl) {
            this.name = name;
            this.platform = platform;
            this.releaseYear = releaseYear;
            this.imageUrl = imageUrl;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getReleaseYear() {
            return releaseYear;
        }

        public void setReleaseYear(String releaseYear) {
            this.releaseYear = releaseYear;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public String getGenre(){
            return "";
        }

    }

}

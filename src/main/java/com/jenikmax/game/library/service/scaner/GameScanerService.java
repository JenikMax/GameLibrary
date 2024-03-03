package com.jenikmax.game.library.service.scaner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameGenre;
import com.jenikmax.game.library.model.entity.Poster;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.scaner.api.ScanerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class GameScanerService implements ScanerService {

    static final Logger logger = LogManager.getLogger(GameScanerService.class.getName());

    private final static String GAME_INFO_PREFIX = "/information";
    private final static String GAME_SCREEN_PREFIX = "/img";
    private final static String GAME_INFO_FILE_NAME = "/information.json";
    private final static String GAME_LOGO_FILE_NAME = "/logo.jpg";



    public List<Game> scanDirectory(String path) {
        List<Game> findGames = new ArrayList<>();
        File directory = new File(path);
        if (!directory.isDirectory()) return findGames;

        List<File> platforms = Arrays.asList(directory.listFiles(File::isDirectory));
        for(File platformDir : platforms){
            String platformName = platformDir.getName();
            logger.info("platformName - {}",platformName);
            List<File> games = Arrays.asList(platformDir.listFiles(File::isDirectory));
            for(File gameDir : games){
                Game game = new Game();
                game.setDirectoryPath(gameDir.getAbsolutePath());
                game.setName(gameDir.getName());
                game.setPlatform(platformName);
                findGames.add(game);
            }
        }
        return findGames;
    }

    public Game getAdditinalGameInfo(Game game){
        File gameinfoDir = new File(game.getDirectoryPath() + GAME_INFO_PREFIX);
        if(gameinfoDir.exists()){
            File logo = new File(game.getDirectoryPath() + GAME_INFO_PREFIX + GAME_LOGO_FILE_NAME);
            if(logo.exists()) game.setPoster(getPoster(game,readImage(logo)));
            File information = new File(game.getDirectoryPath() + GAME_INFO_PREFIX + GAME_INFO_FILE_NAME);
            if(information.exists()){
                GameInfo gameInfo = getAdditinalGameInfo(game.getDirectoryPath() + GAME_INFO_PREFIX + GAME_INFO_FILE_NAME);
                game.setName(gameInfo.getName() != null && !gameInfo.getName().isEmpty() ? gameInfo.getName() : game.getName());
                game.setReleaseDate(gameInfo.getReleaseDate());
                game.setTrailerUrl(gameInfo.getTrailerUrl());
                game.setDescription(gameInfo.getDescription());
                game.setInstruction(gameInfo.getInstruction());
                game.setGenres(new ArrayList<>());
                for(String genre : gameInfo.getGenres()){
                    GameGenre gameGenre = new GameGenre();
                    gameGenre.setGame(game);
                    gameGenre.setGenre(Genre.valueOf(genre));
                    game.getGenres().add(gameGenre);
                }
            }
            File screenDir = new File(game.getDirectoryPath() + GAME_INFO_PREFIX + GAME_SCREEN_PREFIX);
            if(screenDir.exists()){
                List<File> screenImgFiles = Arrays.asList(screenDir.listFiles());
                game.setScreenshots(new ArrayList<>());
                int count = 1;
                for(File img : screenImgFiles){
                    Screenshot screenshot = new Screenshot();
                    screenshot.setGame(game);
                    screenshot.setName("image" + count + "jpg");
                    screenshot.setSource(readImage(img));
                    game.getScreenshots().add(screenshot);
                }
            }

        }
        if(game.getGenres() == null) game.setGenres(new ArrayList<>());
        if(game.getScreenshots() == null) game.setScreenshots(new ArrayList<>());
        if(game.getPoster() == null) game.setPoster(getPoster(game,getDefaultLogo()));
        if(game.getReleaseDate() == null) game.setReleaseDate("N/A");
        if(game.getTrailerUrl() == null) game.setTrailerUrl("N/A");
        if(game.getDescription() == null) game.setDescription("N/A");
        if(game.getInstruction() == null) game.setInstruction("N/A");
        return game;
    }

    private Poster getPoster(Game game, byte[] source){
        Poster poster = new Poster();
        poster.setGame(game);
        poster.setName("poster.jpg");
        poster.setSource(source);
        return poster;
    }

    public void storeGame(Game game){
        this.storeAdditinalGameInfo(game);
        this.storeLogoGameInfo(game);
        this.storeScreenshotsGameInfo(game);
    }


    private void storeAdditinalGameInfo(Game game){
        GameInfo gameInfo = new GameInfo();
        gameInfo.setName(game.getName());
        gameInfo.setPlatform(game.getPlatform());
        gameInfo.setReleaseDate(game.getReleaseDate());
        gameInfo.setGenres(new ArrayList<>());
        for(GameGenre gameGenre : game.getGenres()){
            gameInfo.getGenres().add(gameGenre.getGenre().toString());
        }
        gameInfo.setTrailerUrl(game.getTrailerUrl());
        gameInfo.setDescription(game.getDescription());
        gameInfo.setInstruction(game.getInstruction());
        // Создание объекта ObjectMapper с использованием YAMLFactory
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            File gameInfiFile = getGameInfoFile(game.getDirectoryPath());
            // Запись объекта в YAML файл
            objectMapper.writeValue(gameInfiFile, gameInfo);
            System.out.println("Объект успешно записан в YAML файл.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeLogoGameInfo(Game game){
        try {
            this.saveImage(game.getDirectoryPath() + GAME_INFO_PREFIX, "logo.jpg", game.getPoster().getSource());
        }
        catch (Exception e){
            logger.error("Error storeLogoGameInfo logo - ",e);
        }
    }

    private void storeScreenshotsGameInfo(Game game){
        try {
            for (Screenshot screenshot : game.getScreenshots()) {
                saveImage(game.getDirectoryPath() + GAME_INFO_PREFIX + GAME_SCREEN_PREFIX, screenshot.getName(), screenshot.getSource());
            }
        }
        catch (Exception e){
            logger.error("Error storeScreenshotsGameInfo screenshot - ",e);
        }
    }

    private void saveImage(String directory, String fileName, byte[] imageData) throws IOException {
        Path dirPath = Paths.get(directory);
        Path filePath = Paths.get(directory, fileName);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.write(imageData);
        }
    }

    private File getGameInfoFile(String gameDir) throws IOException {
        File gameInfoFile = new File(gameDir + GAME_INFO_PREFIX + GAME_INFO_FILE_NAME);
        if(!gameInfoFile.exists()){
            gameInfoFile.getParentFile().mkdirs();
            gameInfoFile.createNewFile();
        }
        return gameInfoFile;
    }


    public GameInfo getAdditinalGameInfo(String path){
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Чтение объекта из файла JSON
            GameInfo gameInfo = objectMapper.readValue(new File(path), GameInfo.class);
            return gameInfo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    private byte[] getDefaultLogo(){
        ClassPathResource resource = new ClassPathResource("static/img/default.jpg");
        try {
            return Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            logger.error("GetDefaultImg Error - ",e);
            return null;
        }
    }

    private byte[] readImage(File image){
        try {
            byte[] bytes = Files.readAllBytes(image.toPath());
            return bytes;
        } catch (IOException e) {
            logger.error("GetDefaultImg Error - ", e);
            return null;
        }
    }


    public static class GameInfo{
        private String name;
        private String platform;
        private String releaseDate;
        private String trailerUrl;
        private List<String> genres;
        private String description;
        private String instruction;

        public GameInfo() {
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

        public String getReleaseDate() {
            return releaseDate;
        }

        public void setReleaseDate(String releaseDate) {
            this.releaseDate = releaseDate;
        }

        public List<String> getGenres() {
            return genres;
        }

        public void setGenres(List<String> genres) {
            this.genres = genres;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public String getTrailerUrl() {
            return trailerUrl;
        }

        public void setTrailerUrl(String trailerUrl) {
            this.trailerUrl = trailerUrl;
        }
    }

}

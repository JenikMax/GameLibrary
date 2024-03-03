package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.*;
import com.jenikmax.game.library.model.converter.GameConverter;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameGenre;
import com.jenikmax.game.library.model.entity.Poster;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.data.api.GameService;
import javafx.geometry.Pos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.*;


@Service
public class GameDataService implements GameService {

    static final Logger logger = LogManager.getLogger(GameDataService.class.getName());

    private final SqlDao sqlDao;
    private final GameRepository gameRepository;
    private final GameGenreRepository gameGenreRepository;
    private final PosterRepository posterRepository;
    private final ScreenshotRepository screenshotRepository;

    public GameDataService(SqlDao sqlDao, GameRepository gameRepository, GameGenreRepository gameGenreRepository, PosterRepository posterRepository, ScreenshotRepository screenshotRepository) {
        this.sqlDao = sqlDao;
        this.gameRepository = gameRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.posterRepository = posterRepository;
        this.screenshotRepository = screenshotRepository;
    }



    public List<GameReadDto> getGameShortList(){
        return sqlDao.executeShortGame("select * from v_game_data order by name");
    }

    public List<GameReadDto> getGameShortList(int startIndex, int endIndex){
        return sqlDao.executeShortGame("select * from v_game_data order by name" + (endIndex != 0 ? " OFFSET " + startIndex + " LIMIT " + (endIndex - startIndex) : ""));
    }


    public List<Long> getGameShortIdList(){
        return sqlDao.executeIdGame("select id from v_game_data order by name");
    }

    @Override
    public List<GameReadDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        List<Object> params = new ArrayList<>();
        String sql = "select id, create_ts, name, directory_path, platform, release_date, poster_id from v_game_data where LOWER(name) like LOWER(?)";
        params.add('%' + searchText + '%');
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            sql += String.format(" and platform in (%s)",platformSql);
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            sql += String.format(" and release_date in (%s)",yearsSql);
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            sql += String.format(" and id in (select game_id from library.game_data_genre where genre_code in (%s))",genresSql);
            params.addAll(selectedGenres);
        }
        if(!sortField.isEmpty()){
            if(sortField.equals("year")) {
                sortField = "release_date";
            }
            else if(sortField.equals("create")) {
                sortField = "create_ts";
            }
            else{
                sortField = "name";
            }

            if(sortType == null || sortType.isEmpty() || (!sortType.equals("asc") && !sortType.equals("desc"))){
                sortType = "";
            }
            sql += " order by " + sortField + (sortType.isEmpty() ? "" : " " + sortType);

        }
        else{
            sql += " order by name";
        }
        return sqlDao.executeShortGame(sql,params.toArray());
    }

    @Override
    public List<GameReadDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType,int startIndex, int endIndex) {
        List<Object> params = new ArrayList<>();
        String sql = "select id, create_ts, name, directory_path, platform, release_date, poster_id from v_game_data where LOWER(name) like LOWER(?)";
        params.add('%' + searchText + '%');
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            sql += String.format(" and platform in (%s)",platformSql);
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            sql += String.format(" and release_date in (%s)",yearsSql);
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            sql += String.format(" and id in (select game_id from library.game_data_genre where genre_code in (%s))",genresSql);
            params.addAll(selectedGenres);
        }
        if(!sortField.isEmpty()){
            if(sortField.equals("year")) {
                sortField = "release_date";
            }
            else if(sortField.equals("create")) {
                sortField = "create_ts";
            }
            else{
                sortField = "name";
            }

            if(sortType == null || sortType.isEmpty() || (!sortType.equals("asc") && !sortType.equals("desc"))){
                sortType = "";
            }
            sql += " order by " + sortField + (sortType.isEmpty() ? "" : " " + sortType);

        }
        else{
            sql += " order by name";
        }
        if (endIndex != 0){
            sql += " OFFSET " + startIndex + " LIMIT " + (endIndex - startIndex);
        }
        return sqlDao.executeShortGame(sql,params.toArray());
    }

    @Override
    public List<Long> getGameShortIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        List<Object> params = new ArrayList<>();
        String sql = "select id from v_game_data where LOWER(name) like LOWER(?)";
        params.add('%' + searchText + '%');
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            sql += String.format(" and platform in (%s)",platformSql);
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            sql += String.format(" and release_date in (%s)",yearsSql);
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            sql += String.format(" and id in (select game_id from library.game_data_genre where genre_code in (%s))",genresSql);
            params.addAll(selectedGenres);
        }
        if(!sortField.isEmpty()){
            if(sortField.equals("year")) {
                sortField = "release_date";
            }
            else if(sortField.equals("create")) {
                sortField = "create_ts";
            }
            else{
                sortField = "name";
            }

            if(sortType == null || sortType.isEmpty() || (!sortType.equals("asc") && !sortType.equals("desc"))){
                sortType = "";
            }
            sql += " order by " + sortField + (sortType.isEmpty() ? "" : " " + sortType);

        }
        else{
            sql += " order by name";
        }
        return sqlDao.executeShortGameId(sql,params.toArray());
    }

    @Override
    public List<String> getReleaseDates() {
        return sqlDao.executeByStringList("select release_date from library.game_data group by release_date order by release_date desc","release_date");
    }

    @Override
    public List<String> getGamesPlatforms() {
        return sqlDao.executeByStringList("select platform from library.game_data group by platform order by platform","platform");
    }

    @Override
    public List<Genre> getGenres() {
        return sqlDao.getGenreList("select code from library.game_genre group by code order by description","code");
    }

    public List<Genre> getGenres(Locale locale){
        if(locale.toString().equals("ru")){
            return sqlDao.getGenreList("select code from library.game_genre group by code order by description_ru","code");
        }
        else{
            return getGenres();
        }
    }


    @Override
    public List<String> getGameGenres() {
        return sqlDao.executeByLowerStringList("select code from library.game_genre group by code order by description","code");
    }

    @Override
    public byte[] getImageBytesById(Long id) {
        Screenshot screenshot = screenshotRepository.getReferenceById(id);
        return screenshot != null ? screenshot.getSource() : null;
    }

    @Override
    public byte[] getPosterBytesById(Long id){
        Poster poster = posterRepository.getReferenceById(id);
        return poster != null ? poster.getSource() : null;
    }


    @Transactional
    public GameDto testCreate() {
        Game game = new Game();
        game.setCreateTs(new Timestamp(new Date().getTime()));
        game.setName("name");
        game.setDirectoryPath("directory pth");
        game.setReleaseDate("releaseDate");
        game.setTrailerUrl("trailerUrl");
        game.setPlatform("platform");
        game.setDescription("description");
        game.setInstruction("instruction");
        //game.setLogo(getDefaultImg());
        List<GameGenre> genres = new ArrayList<>();
        game.setGenres(genres);

        GameGenre gameGenre = new GameGenre();
        gameGenre.setGame(game);
        //gameGenre.setGenre(Genre.JRPG);
        game.getGenres().add(gameGenre);
        gameGenre = new GameGenre();
        gameGenre.setGame(game);
        //gameGenre.setGenre(Genre.ACTION);
        game.getGenres().add(gameGenre);


        List<Screenshot> screenshots = new ArrayList<>();
        game.setScreenshots(screenshots);
        Screenshot screenshot = new Screenshot();
        screenshot.setGame(game);
        screenshot.setName("img.jpg");
        //screenshot.setSource(getDefaultImg());
        game.getScreenshots().add(screenshot);

        game = gameRepository.save(game);
        game = gameRepository.getReferenceById(game.getId());
        return GameConverter.gameToDtoConverter(game);
    }


    @Override
    public List<Game> getGameList() {
        return gameRepository.findAll();
    }

    @Override
    public Game getGameById(Long gameId) {
        return gameRepository.getReferenceById(gameId);
    }

    @Transactional
    @Override
    public void storeGame(Game game) {
        game.setCreateTs(new Timestamp(new Date().getTime()));
        gameRepository.save(game);
    }

    @Transactional
    @Override
    public void deleteGameInfo(Long id){
        gameRepository.deleteById(id);
    }


    @Transactional
    @Override
    public void updateGame(Game game) {
        gameRepository.save(game);
    }

    @Transactional
    @Override
    public void storeNewGameInLibrary(List<Game> games) {
        for(Game gameShort : games){
            if(gameShort.getGenres() == null) gameShort.setGenres(new ArrayList<>());
            if(gameShort.getPoster() == null) gameShort.setPoster(getDefaultPoster(gameShort));
            if(gameShort.getReleaseDate() == null) gameShort.setReleaseDate("N/A");
        }
    }

    private Poster getDefaultPoster(Game game){
        Poster poster = new Poster();
        poster.setName("poster.jpg");
        poster.setGame(game);
        poster.setSource(getDefaultLogo());
        return poster;
    }

    private byte[] getDefaultLogo(){
        ClassPathResource resource = new ClassPathResource("static/img/logo.jpg");
        try {
            return Files.readAllBytes(resource.getFile().toPath());
        } catch (IOException e) {
            logger.error("GetDefaultImg Error - ",e);
            return null;
        }
    }
}

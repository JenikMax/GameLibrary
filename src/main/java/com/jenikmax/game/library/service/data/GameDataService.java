package com.jenikmax.game.library.service.data;

import com.jenikmax.game.library.dao.api.GameGenreRepository;
import com.jenikmax.game.library.dao.api.GameRepository;
import com.jenikmax.game.library.dao.api.ScreenshotRepository;
import com.jenikmax.game.library.dao.api.SqlDao;
import com.jenikmax.game.library.model.converter.GameConverter;
import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.GameGenre;
import com.jenikmax.game.library.model.entity.Screenshot;
import com.jenikmax.game.library.model.entity.enums.Genre;
import com.jenikmax.game.library.service.data.api.GameService;
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
    private final ScreenshotRepository screenshotRepository;

    public GameDataService(SqlDao sqlDao, GameRepository gameRepository, GameGenreRepository gameGenreRepository, ScreenshotRepository screenshotRepository) {
        this.sqlDao = sqlDao;
        this.gameRepository = gameRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.screenshotRepository = screenshotRepository;
    }



    public List<GameShortDto> getGameShortList(){
        return sqlDao.executeShortGame("select * from game_data");
    }

    @Override
    public List<GameShortDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres) {
        List<Object> params = new ArrayList<>();
        String sql = "select id, create_ts, name, directory_path, platform, release_date, logo from game_data where name like ?";
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
        return sqlDao.executeShortGame(sql,params.toArray());
    }

    @Override
    public List<String> getReleaseDates() {
        return sqlDao.executeByStringList("select release_date from library.game_data group by release_date","release_date");
    }

    @Override
    public List<String> getGamesPlatforms() {
        return sqlDao.executeByStringList("select platform from library.game_data group by platform","platform");
    }

    @Override
    public List<String> getGameGenres() {
        return sqlDao.executeByLowerStringList("select code from library.game_genre group by code","code");
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
        gameGenre.setGenre(Genre.JRPG);
        game.getGenres().add(gameGenre);
        gameGenre = new GameGenre();
        gameGenre.setGame(game);
        gameGenre.setGenre(Genre.ACTION);
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
    public void storeNewGameInLibrary(List<Game> games) {
        for(Game gameShort : games){
            if(gameShort.getGenres() == null) gameShort.setGenres(new ArrayList<>());
            if(gameShort.getLogo() == null) gameShort.setLogo(getDefaultLogo());
            if(gameShort.getReleaseDate() == null) gameShort.setReleaseDate("N/A");
        }
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

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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager entityManager;

    public GameDataService(SqlDao sqlDao, GameRepository gameRepository, GameGenreRepository gameGenreRepository, ScreenshotRepository screenshotRepository) {
        this.sqlDao = sqlDao;
        this.gameRepository = gameRepository;
        this.gameGenreRepository = gameGenreRepository;
        this.screenshotRepository = screenshotRepository;
    }



    public List<GameShortDto> getGameShortList(){
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                "from game_data g left join library.game_data_genre dg on dg.game_id = g.id " +
                "group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo order by g.name";
        return sqlDao.executeShortGame(sql);
    }

    public List<GameShortDto> getGameShortList(int startIndex, int endIndex){
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                "from game_data g left join library.game_data_genre dg on dg.game_id = g.id " +
                "group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo order by g.name" +
                (endIndex != 0 ? " offset " + startIndex + " limit " + (endIndex - startIndex) : "");
        return sqlDao.executeShortGame(sql);
    }


    public List<Long> getGameShortIdList(){
        return sqlDao.executeIdGame("select id from game_data order by name");
    }

    @Override
    public List<GameShortDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        List<Object> params = new ArrayList<>();
        String from = "from game_data g left join library.game_data_genre dg on dg.game_id = g.id";
        String where = buildSearchFilter(searchText, params);
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            where += " and g.platform in (" + platformSql + ")";
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            where += " and g.release_date in (" + yearsSql + ")";
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            where += " and g.id in (select game_id from library.game_data_genre where genre_code in (" + genresSql + "))";
            params.addAll(selectedGenres);
        }
        String order = buildOrderClause(sortField, sortType);
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                from + where + " group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo" + order;
        return sqlDao.executeShortGame(sql, params.toArray());
    }

    @Override
    public List<GameShortDto> getGameShortList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType,int startIndex, int endIndex) {
        List<Object> params = new ArrayList<>();
        String from = "from game_data g left join library.game_data_genre dg on dg.game_id = g.id";
        String where = buildSearchFilter(searchText, params);
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            where += " and g.platform in (" + platformSql + ")";
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            where += " and g.release_date in (" + yearsSql + ")";
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            where += " and g.id in (select game_id from library.game_data_genre where genre_code in (" + genresSql + ") group by game_id having count(distinct genre_code) = " + selectedGenres.size() + ")";
            params.addAll(selectedGenres);
        }
        String order = buildOrderClause(sortField, sortType);
        String limit = (endIndex != 0) ? " offset " + startIndex + " limit " + (endIndex - startIndex) : "";
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                from + where + " group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo" + order + limit;
        return sqlDao.executeShortGame(sql, params.toArray());
    }

    @Override
    public List<Long> getGameShortIdList(String searchText, List<String> selectedPlatforms, List<String> selectedYears, List<String> selectedGenres, String sortField, String sortType) {
        List<Object> params = new ArrayList<>();
        String from = "from game_data g";
        String where = buildSearchFilter(searchText, params);
        if(selectedPlatforms.size() != 0){
            String platformSql = String.join(",", Collections.nCopies(selectedPlatforms.size(), "?"));
            where += " and g.platform in (" + platformSql + ")";
            params.addAll(selectedPlatforms);
        }
        if(selectedYears.size() != 0){
            String yearsSql = String.join(",", Collections.nCopies(selectedYears.size(), "?"));
            where += " and g.release_date in (" + yearsSql + ")";
            params.addAll(selectedYears);
        }
        if(selectedGenres.size() != 0){
            String genresSql = String.join(",", Collections.nCopies(selectedGenres.size(), "?"));
            where += " and g.id in (select game_id from library.game_data_genre where genre_code in (" + genresSql + ") group by game_id having count(distinct genre_code) = " + selectedGenres.size() + ")";
            params.addAll(selectedGenres);
        }
        String order = buildOrderClause(sortField, sortType);
        String sql = "select g.id" + from + where + order;
        return sqlDao.executeShortGameId(sql, params.toArray());
    }

    // ─── helpers ───────────────────────────────────────────────────────────────

    private String buildSearchFilter(String searchText, List<Object> params) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return "";
        }
        params.add(searchText.trim());
        return " where g.search_vector @@ plainto_tsquery('russian', ?)";
    }

    private String buildOrderClause(String sortField, String sortType) {
        if (sortField == null || sortField.isEmpty()) return " order by g.name";
        switch (sortField) {
            case "year":  sortField = "g.release_date"; break;
            case "create": sortField = "g.create_ts"; break;
            default:       sortField = "g.name";
        }
        boolean desc = sortType != null && sortType.equals("desc");
        return " order by " + sortField + (desc ? " desc" : "");

    @Override
    public List<String> getReleaseDates() {
        return sqlDao.executeByStringList("select release_date from library.game_data group by release_date order by release_date","release_date");
    }

    @Override
    public List<String> getGamesPlatforms() {
        return sqlDao.executeByStringList("select platform from library.game_data group by platform order by platform","platform");
    }

    @Override
    public List<Genre> getGenres() {
        return sqlDao.getGenreList("select code from library.game_genre group by code order by description_ru","code");
    }

    public List<Genre> getGenres(Locale locale){
        if(locale.getLanguage().equals("ru")){
            return sqlDao.getGenreList("select code from library.game_genre group by code order by description_ru","code");
        }
        else{
            return sqlDao.getGenreList("select code from library.game_genre group by code order by description","code");
        }
    }


    @Override
    public Long findRandomGameId() {
        return sqlDao.executeIdGame("SELECT id FROM library.game_data ORDER BY RANDOM() LIMIT 1").stream().findFirst().orElse(null);
    }

    @Override
    public List<String> getGameGenres() {
        return sqlDao.executeByLowerStringList("select code from library.game_genre group by code order by description","code");
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
    public List<Object[]> getGameDirectoryPaths() {
        return entityManager.createQuery("select g.id, g.directoryPath from Game g")
                .getResultList();
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
    public Game storeGameMetadata(Game game) {
        game.setCreateTs(new Timestamp(new Date().getTime()));
        game.setLogo(null);
        game.setScreenshots(new ArrayList<>());
        return gameRepository.save(game);
    }

    @Transactional
    @Override
    public void updateGameImages(Game game) {
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
            if(gameShort.getLogo() == null) gameShort.setLogo(getDefaultLogo());
            if(gameShort.getReleaseDate() == null) gameShort.setReleaseDate("N/A");
        }
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
}

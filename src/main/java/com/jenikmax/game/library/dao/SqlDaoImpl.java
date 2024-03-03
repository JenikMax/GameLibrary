package com.jenikmax.game.library.dao;

import com.jenikmax.game.library.dao.api.SqlDao;
import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.enums.Genre;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Repository
public class SqlDaoImpl implements SqlDao {

    static final Logger logger = LogManager.getLogger(SqlDaoImpl.class.getName());
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @Override
    public List<GameReadDto> executeShortGame(String query) {
        List<GameReadDto> gameShortDtoList = jdbcTemplate.query(query, (rs, rowNum) -> {
            GameReadDto dto = new GameReadDto();
            dto.setId(rs.getLong("id"));
            dto.setCreateTs(rs.getTimestamp("create_ts"));
            dto.setName(rs.getString("name"));
            dto.setDirectoryPath(rs.getString("directory_path"));
            dto.setPlatform(rs.getString("platform"));
            dto.setReleaseDate(rs.getString("release_date"));
            dto.setGenres(new ArrayList<>());
            dto.setLogo(rs.getLong("poster_id"));
            return dto;
        });
        return gameShortDtoList;
    }

    @Override
    public List<Long> executeIdGame(String query) {
        List<Long> gameIdList = jdbcTemplate.query(query, (rs, rowNum) -> rs.getLong("id"));
        return gameIdList;
    }

    @Override
    public List<String> executeByStringList(String query, String column) {
        return jdbcTemplate.query(query, (rs, rowNum) -> rs.getString(column));
    }

    @Override
    public List<Genre> getGenreList(String query, String column) {
        return jdbcTemplate.query(query, (rs, rowNum) ->  Genre.valueOf(rs.getString(column)));
    }

    @Override
    public List<String> executeByLowerStringList(String query, String column) {
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            String result = rs.getString(column);
            return result != null ? result.toLowerCase() : "";
        });
    }

    @Override
    public List<GameReadDto> executeShortGame(String query, Object[] params) {
        List<GameReadDto> gameShortDtoList = jdbcTemplate.query(query, (rs, rowNum) -> {
            GameReadDto dto = new GameReadDto();
            dto.setId(rs.getLong("id"));
            dto.setCreateTs(rs.getTimestamp("create_ts"));
            dto.setName(rs.getString("name"));
            dto.setDirectoryPath(rs.getString("directory_path"));
            dto.setPlatform(rs.getString("platform"));
            dto.setReleaseDate(rs.getString("release_date"));
            dto.setGenres(new ArrayList<>());
            dto.setLogo(rs.getLong("poster_id"));
            return dto;
        }, params);
        return gameShortDtoList;
    }

    @Override
    public List<Long> executeShortGameId(String query, Object[] params) {
        List<Long> gameIdList = jdbcTemplate.query(query, (rs, rowNum) -> rs.getLong("id"), params);
        return gameIdList;
    }

    @Override
    public List<Map<String, Object>> execute(String query) {
        return jdbcTemplate.query(query, new ColumnMapRowMapper());
    }

    @Override
    public List<Map<String, Object>> execute(String query, Object[] params) {
        return jdbcTemplate.query(query, new ColumnMapRowMapper(), params);
    }

    @Override
    public int executeUpdate(String query, Map<String, Object> params) {
        return namedParameterJdbcTemplate.update(query, params);
    }

    @Override
    public int executeUpdate(String query, Object[] params) {
        return jdbcTemplate.update(query, params);
    }
}

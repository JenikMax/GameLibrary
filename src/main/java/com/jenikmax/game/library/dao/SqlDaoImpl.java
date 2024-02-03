package com.jenikmax.game.library.dao;

import com.jenikmax.game.library.dao.api.SqlDao;
import com.jenikmax.game.library.model.dto.GameShortDto;
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
    public List<GameShortDto> executeShortGame(String query) {
        List<GameShortDto> gameShortDtoList = jdbcTemplate.query(query, (rs, rowNum) -> {
            GameShortDto dto = new GameShortDto();
            dto.setId(rs.getLong("id"));
            dto.setCreateTs(rs.getTimestamp("create_ts"));
            dto.setName(rs.getString("name"));
            dto.setDirectoryPath(rs.getString("directory_path"));
            dto.setPlatform(rs.getString("platform"));
            dto.setReleaseDate(rs.getString("release_date"));
            dto.setGenres(new ArrayList<>());
            dto.setLogo("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(rs.getBytes("logo")));
            return dto;
        });
        return gameShortDtoList;
    }

    @Override
    public List<GameShortDto> executeShortGame(String query, Object[] params) {
        return null;
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

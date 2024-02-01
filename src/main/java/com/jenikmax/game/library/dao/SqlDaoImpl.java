package com.jenikmax.game.library.dao;

import com.jenikmax.game.library.dao.api.SqlDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

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

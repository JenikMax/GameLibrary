package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.dto.GameReadDto;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.List;
import java.util.Map;

public interface SqlDao {

    List<GameReadDto> executeShortGame(String query);

    List<Long> executeIdGame(String query);

    List<String> executeByStringList(String query, String column);

    List<Genre> getGenreList(String query, String column);

    List<String> executeByLowerStringList(String query, String column);

    List<GameReadDto> executeShortGame(String query,  Object[] params);

    List<Long> executeShortGameId(String query,  Object[] params);

    List<Map<String, Object>> execute(String query);

    List<Map<String, Object>> execute(String query, Object[] params);

    int executeUpdate(String query, Map<String, Object> params);

    int executeUpdate(String query, Object[] params);
}

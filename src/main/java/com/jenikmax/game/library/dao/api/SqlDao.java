package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.dto.GameShortDto;

import java.util.List;
import java.util.Map;

public interface SqlDao {

    List<GameShortDto> executeShortGame(String query);

    List<String> executeByStringList(String query, String column);

    List<String> executeByLowerStringList(String query, String column);

    List<GameShortDto> executeShortGame(String query,  Object[] params);

    List<Map<String, Object>> execute(String query);

    List<Map<String, Object>> execute(String query, Object[] params);

    int executeUpdate(String query, Map<String, Object> params);

    int executeUpdate(String query, Object[] params);
}

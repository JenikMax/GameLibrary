package com.jenikmax.game.library.dao.api;

import java.util.List;
import java.util.Map;

public interface SqlDao {

    List<Map<String, Object>> execute(String query);

    List<Map<String, Object>> execute(String query, Object[] params);

    int executeUpdate(String query, Map<String, Object> params);

    int executeUpdate(String query, Object[] params);
}

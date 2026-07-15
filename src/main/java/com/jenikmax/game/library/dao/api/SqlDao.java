package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.List;
import java.util.Map;

public interface SqlDao {

    /**
     * @deprecated Use parameterized version {@link #executeShortGame(String, Object[])} instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<GameShortDto> executeShortGame(String query);

    /**
     * @deprecated Use parameterized version {@link #executeShortGameId(String, Object[])} instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<Long> executeIdGame(String query);

    /**
     * @deprecated Use parameterized version instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<String> executeByStringList(String query, String column);

    /**
     * @deprecated Use parameterized version instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<Genre> getGenreList(String query, String column);

    /**
     * @deprecated Use parameterized version instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<String> executeByLowerStringList(String query, String column);

    List<GameShortDto> executeShortGame(String query, Object[] params);

    List<Long> executeShortGameId(String query, Object[] params);

    /**
     * @deprecated Use parameterized version {@link #execute(String, Object[])} instead.
     * Only safe when query string contains NO user-supplied values.
     */
    @Deprecated
    List<Map<String, Object>> execute(String query);

    List<Map<String, Object>> execute(String query, Object[] params);

    int executeUpdate(String query, Map<String, Object> params);

    int executeUpdate(String query, Object[] params);
}

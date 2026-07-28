package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.entity.enums.Genre;

import java.util.List;
import java.util.Map;

/**
 * DAO-интерфейс для выполнения произвольных SQL-запросов через JdbcTemplate.
 * Предоставляет методы для маппинга результатов в GameShortDto, списки ID,
 * строковые списки, Map, а также для выполнения UPDATE/INSERT/DELETE.
 */
public interface SqlDao {

    /**
     * Выполняет SQL-запрос и возвращает список GameShortDto.
     * @deprecated Используйте параметризованную версию {@link #executeShortGame(String, Object[])}.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<GameShortDto> executeShortGame(String query);

    /**
     * Выполняет SQL-запрос и возвращает список ID игр.
     * @deprecated Используйте параметризованную версию {@link #executeShortGameId(String, Object[])}.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<Long> executeIdGame(String query);

    /**
     * Выполняет SQL-запрос и возвращает список строк из указанной колонки.
     * @deprecated Используйте параметризованную версию.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<String> executeByStringList(String query, String column);

    /**
     * Выполняет SQL-запрос и возвращает список жанров (Genre) из указанной колонки.
     * @deprecated Используйте параметризованную версию.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<Genre> getGenreList(String query, String column);

    /**
     * Выполняет SQL-запрос и возвращает список строк из указанной колонки в нижнем регистре.
     * @deprecated Используйте параметризованную версию.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<String> executeByLowerStringList(String query, String column);

    /**
     * Выполняет параметризованный SQL-запрос и возвращает список GameShortDto.
     * @param query  SQL-запрос с плейсхолдерами (?)
     * @param params массив параметров запроса
     * @return список кратких DTO игр
     */
    List<GameShortDto> executeShortGame(String query, Object[] params);

    /**
     * Выполняет параметризованный SQL-запрос и возвращает список ID игр.
     * @param query  SQL-запрос с плейсхолдерами
     * @param params массив параметров
     * @return список ID игр
     */
    List<Long> executeShortGameId(String query, Object[] params);

    /**
     * Выполняет SQL-запрос и возвращает список строк как Map.
     * @deprecated Используйте параметризованную версию {@link #execute(String, Object[])}.
     * Безопасен только если строка запроса НЕ содержит пользовательских значений.
     */
    @Deprecated
    List<Map<String, Object>> execute(String query);

    /**
     * Выполняет параметризованный SQL-запрос и возвращает список строк как Map.
     * @param query  SQL-запрос с плейсхолдерами
     * @param params массив параметров
     * @return список Map (ключ — имя колонки, значение — ячейка)
     */
    List<Map<String, Object>> execute(String query, Object[] params);

    /**
     * Выполняет UPDATE/INSERT/DELETE с именованными параметрами.
     * @param query  SQL-запрос с :именованными параметрами
     * @param params карта имя_параметра → значение
     * @return количество затронутых строк
     */
    int executeUpdate(String query, Map<String, Object> params);

    /**
     * Выполняет UPDATE/INSERT/DELETE с позиционными параметрами.
     * @param query  SQL-запрос с плейсхолдерами (?)
     * @param params массив параметров
     * @return количество затронутых строк
     */
    int executeUpdate(String query, Object[] params);
}

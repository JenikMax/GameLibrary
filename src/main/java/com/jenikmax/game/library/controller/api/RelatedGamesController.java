package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.dao.api.SqlDao;
import com.jenikmax.game.library.model.dto.GameShortDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/games")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Related Games", description = "Related games by genre and series")
public class RelatedGamesController {

    private final SqlDao sqlDao;

    public RelatedGamesController(SqlDao sqlDao) {
        this.sqlDao = sqlDao;
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRelatedGames(@PathVariable Long id) {
        List<GameShortDto> sameGenre = findByGenre(id);
        List<GameShortDto> sameSeries = findBySeries(id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sameGenre", toSimpleList(sameGenre));
        result.put("sameSeries", toSimpleList(sameSeries));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    private List<GameShortDto> findByGenre(Long gameId) {
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                "from game_data g " +
                "join library.game_data_genre dg on dg.game_id = g.id " +
                "where g.id != ? " +
                "and dg.genre_code in (select genre_code from library.game_data_genre where game_id = ?) " +
                "group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo " +
                "order by count(*) desc, g.name limit 6";
        return sqlDao.executeShortGame(sql, new Object[]{gameId, gameId});
    }

    private List<GameShortDto> findBySeries(Long gameId) {
        // extract base name: strip brackets, then trailing version numbers
        String baseSql = "select coalesce(" +
                "nullif(regexp_replace(name, '\\s*\\(.*\\)$', ''), ''), " +
                "nullif(regexp_replace(name, '\\s*\\[.*\\]$', ''), ''), " +
                "nullif(regexp_replace(name, '\\s+\\d+.*$', ''), ''), " +
                "name" +
                ") from game_data where id = ?";
        List<Map<String, Object>> baseResult = sqlDao.execute(baseSql, new Object[]{gameId});
        if (baseResult.isEmpty()) return Collections.emptyList();

        String baseName = (String) baseResult.get(0).values().iterator().next();
        if (baseName == null || baseName.trim().isEmpty()) return Collections.emptyList();
        baseName = baseName.trim();

        List<GameShortDto> results = findByNamePrefix(gameId, baseName);
        // if only found itself (or nothing), fallback to first-word matching
        if (results.size() <= 1) {
            String firstWord = baseName.contains(" ") ? baseName.substring(0, baseName.indexOf(' ')) : baseName;
            if (!firstWord.equals(baseName)) {
                results = findByNamePrefix(gameId, firstWord);
            }
        }
        return results;
    }

    private List<GameShortDto> findByNamePrefix(Long gameId, String prefix) {
        String sql = "select g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo, " +
                "string_agg(dg.genre_code, ',' order by dg.genre_code) filter (where dg.genre_code is not null) as genre_codes " +
                "from game_data g " +
                "left join library.game_data_genre dg on dg.game_id = g.id " +
                "where g.id != ? and g.name ilike ? " +
                "group by g.id, g.create_ts, g.name, g.directory_path, g.platform, g.release_date, g.logo " +
                "order by g.name limit 6";
        return sqlDao.executeShortGame(sql, new Object[]{gameId, prefix + "%"});
    }

    private List<Map<String, Object>> toSimpleList(List<GameShortDto> dtos) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (GameShortDto dto : dtos) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", dto.getId());
            m.put("name", dto.getName());
            m.put("platform", dto.getPlatform());
            m.put("releaseDate", dto.getReleaseDate());
            m.put("genres", dto.getGenres());
            list.add(m);
        }
        return list;
    }
}

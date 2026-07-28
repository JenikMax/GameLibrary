package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Screenshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы со скриншотами игр.
 */
public interface ScreenshotRepository extends JpaRepository<Screenshot,Long> {

    /**
     * Возвращает список ID скриншотов, привязанных к игре.
     * @param gameId ID игры
     * @return список ID скриншотов
     */
    @Query("SELECT s.id FROM Screenshot s WHERE s.game.id = :gameId ORDER BY s.id")
    List<Long> findIdsByGameId(@Param("gameId") Long gameId);

    /**
     * Удаляет все скриншоты, привязанные к игре.
     * @param gameId ID игры
     */
    void deleteByGameId(Long gameId);
}

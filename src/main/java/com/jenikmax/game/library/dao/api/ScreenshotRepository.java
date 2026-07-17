package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Screenshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScreenshotRepository extends JpaRepository<Screenshot,Long> {

    @Query("SELECT s.id FROM Screenshot s WHERE s.game.id = :gameId ORDER BY s.id")
    List<Long> findIdsByGameId(@Param("gameId") Long gameId);

    void deleteByGameId(Long gameId);
}

package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;


/**
 * Репозиторий для базовых CRUD-операций с сущностью Game.
 * Специфические запросы выполняются через SqlDao (JdbcTemplate).
 */
public interface GameRepository  extends JpaRepository<Game, Long> {
}

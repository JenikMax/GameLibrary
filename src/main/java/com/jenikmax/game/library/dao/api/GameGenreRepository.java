package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameGenre;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы со связями игр и жанров (таблица game_data_genre).
 */
public interface GameGenreRepository extends JpaRepository<GameGenre, Long> {

    /**
     * Удаляет все жанры, привязанные к игре.
     * @param gameId ID игры
     */
    void deleteByGameId(Long gameId);
}

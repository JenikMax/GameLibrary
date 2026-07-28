package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.FavoriteGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с избранными играми пользователей.
 * Предоставляет методы для проверки, добавления и удаления игр из избранного,
 * а также для подсчёта количества избранных игр у пользователя и игры.
 */
public interface FavoriteGameRepository extends JpaRepository<FavoriteGame, Long> {

    /**
     * Находит запись избранного по ID пользователя и ID игры.
     * @param userId ID пользователя
     * @param gameId ID игры
     * @return Optional с записью избранного или пустой
     */
    Optional<FavoriteGame> findByUserIdAndGameId(Long userId, Long gameId);

    /**
     * Возвращает список ID игр, добавленных пользователем в избранное.
     * @param userId ID пользователя
     * @return список ID игр
     */
    @Query("SELECT f.game.id FROM FavoriteGame f WHERE f.user.id = :userId")
    List<Long> findGameIdsByUserId(@Param("userId") Long userId);

    /**
     * Проверяет, добавил ли пользователь игру в избранное.
     * @param userId ID пользователя
     * @param gameId ID игры
     * @return true если игра в избранном
     */
    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    /**
     * Возвращает количество пользователей, добавивших игру в избранное.
     * @param gameId ID игры
     * @return количество избранных
     */
    long countByGameId(Long gameId);

    /**
     * Возвращает количество избранных игр у пользователя.
     * @param userId ID пользователя
     * @return количество избранных игр
     */
    long countByUserId(Long userId);
}

package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с оценками игр (рейтинг 1–10).
 */
public interface GameRatingRepository extends JpaRepository<GameRating, Long> {

    /**
     * Вычисляет среднюю оценку игры.
     * @param gameId ID игры
     * @return средняя оценка или null
     */
    @Query("SELECT AVG(r.rating) FROM GameRating r WHERE r.game.id = :gameId")
    Double findAvgRatingByGameId(@Param("gameId") Long gameId);

    /**
     * Подсчитывает количество оценок у игры.
     * @param gameId ID игры
     * @return количество оценок
     */
    @Query("SELECT COUNT(r) FROM GameRating r WHERE r.game.id = :gameId")
    Long countRatingsByGameId(@Param("gameId") Long gameId);

    /**
     * Возвращает оценку пользователя для указанной игры.
     * @param gameId ID игры
     * @param userId ID пользователя
     * @return Optional с оценкой или пустой
     */
    @Query("SELECT r.rating FROM GameRating r WHERE r.game.id = :gameId AND r.user.id = :userId")
    Optional<Integer> findUserRating(@Param("gameId") Long gameId, @Param("userId") Long userId);

    /**
     * Находит запись оценки по ID игры и ID пользователя.
     * @param gameId ID игры
     * @param userId ID пользователя
     * @return Optional с оценкой или пустой
     */
    Optional<GameRating> findByGameIdAndUserId(Long gameId, Long userId);

    /**
     * Возвращает средние оценки и количество оценок для нескольких игр.
     * @param gameIds список ID игр
     * @return список Object[]: [gameId, avgRating, count]
     */
    @Query("SELECT r.game.id, AVG(r.rating), COUNT(r) FROM GameRating r WHERE r.game.id IN :ids GROUP BY r.game.id")
    List<Object[]> findAvgRatingByGameIds(@Param("ids") List<Long> gameIds);

    /**
     * Подсчитывает количество оценок, поставленных пользователем.
     * @param userId ID пользователя
     * @return количество оценок
     */
    long countByUserId(Long userId);
}

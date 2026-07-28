package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с рецензиями на игры.
 * Каждый пользователь может оставить не более одной рецензии на игру.
 * Рецензия содержит оценки по 4 категориям (геймплей, графика, сюжет, музыка) и текст.
 */
public interface GameReviewRepository extends JpaRepository<GameReview, Long> {

    /**
     * Находит все рецензии на игру, отсортированные по дате создания (от новых к старым).
     * @param gameId ID игры
     * @return список рецензий
     */
    List<GameReview> findByGameIdOrderByCreatedAtDesc(Long gameId);

    /**
     * Находит рецензию пользователя на игру.
     * @param gameId ID игры
     * @param userId ID пользователя
     * @return Optional с рецензией или пустой
     */
    Optional<GameReview> findByGameIdAndUserId(Long gameId, Long userId);

    /**
     * Подсчитывает количество рецензий у игры.
     * @param gameId ID игры
     * @return количество рецензий
     */
    long countByGameId(Long gameId);

    /**
     * Подсчитывает количество рецензий у пользователя.
     * @param userId ID пользователя
     * @return количество рецензий
     */
    long countByUserId(Long userId);

    /**
     * Вычисляет среднюю оценку геймплея для игры.
     * @param gameId ID игры
     * @return средняя оценка или null
     */
    @Query("SELECT AVG(r.gameplayScore) FROM GameReview r WHERE r.game.id = :gameId AND r.gameplayScore IS NOT NULL")
    Double findAvgGameplayScore(@Param("gameId") Long gameId);

    /**
     * Вычисляет среднюю оценку графики для игры.
     * @param gameId ID игры
     * @return средняя оценка или null
     */
    @Query("SELECT AVG(r.graphicsScore) FROM GameReview r WHERE r.game.id = :gameId AND r.graphicsScore IS NOT NULL")
    Double findAvgGraphicsScore(@Param("gameId") Long gameId);

    /**
     * Вычисляет среднюю оценку сюжета для игры.
     * @param gameId ID игры
     * @return средняя оценка или null
     */
    @Query("SELECT AVG(r.storyScore) FROM GameReview r WHERE r.game.id = :gameId AND r.storyScore IS NOT NULL")
    Double findAvgStoryScore(@Param("gameId") Long gameId);

    /**
     * Вычисляет среднюю оценку музыки для игры.
     * @param gameId ID игры
     * @return средняя оценка или null
     */
    @Query("SELECT AVG(r.musicScore) FROM GameReview r WHERE r.game.id = :gameId AND r.musicScore IS NOT NULL")
    Double findAvgMusicScore(@Param("gameId") Long gameId);
}

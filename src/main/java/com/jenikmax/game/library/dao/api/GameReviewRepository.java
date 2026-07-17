package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameReviewRepository extends JpaRepository<GameReview, Long> {

    List<GameReview> findByGameIdOrderByCreatedAtDesc(Long gameId);

    Optional<GameReview> findByGameIdAndUserId(Long gameId, Long userId);

    long countByGameId(Long gameId);

    @Query("SELECT AVG(r.gameplayScore) FROM GameReview r WHERE r.game.id = :gameId AND r.gameplayScore IS NOT NULL")
    Double findAvgGameplayScore(@Param("gameId") Long gameId);

    @Query("SELECT AVG(r.graphicsScore) FROM GameReview r WHERE r.game.id = :gameId AND r.graphicsScore IS NOT NULL")
    Double findAvgGraphicsScore(@Param("gameId") Long gameId);

    @Query("SELECT AVG(r.storyScore) FROM GameReview r WHERE r.game.id = :gameId AND r.storyScore IS NOT NULL")
    Double findAvgStoryScore(@Param("gameId") Long gameId);

    @Query("SELECT AVG(r.musicScore) FROM GameReview r WHERE r.game.id = :gameId AND r.musicScore IS NOT NULL")
    Double findAvgMusicScore(@Param("gameId") Long gameId);
}

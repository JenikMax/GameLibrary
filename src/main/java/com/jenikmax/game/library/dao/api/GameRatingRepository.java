package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRatingRepository extends JpaRepository<GameRating, Long> {

    @Query("SELECT AVG(r.rating) FROM GameRating r WHERE r.game.id = :gameId")
    Double findAvgRatingByGameId(@Param("gameId") Long gameId);

    @Query("SELECT COUNT(r) FROM GameRating r WHERE r.game.id = :gameId")
    Long countRatingsByGameId(@Param("gameId") Long gameId);

    @Query("SELECT r.rating FROM GameRating r WHERE r.game.id = :gameId AND r.user.id = :userId")
    Optional<Integer> findUserRating(@Param("gameId") Long gameId, @Param("userId") Long userId);

    Optional<GameRating> findByGameIdAndUserId(Long gameId, Long userId);

    @Query("SELECT r.game.id, AVG(r.rating), COUNT(r) FROM GameRating r WHERE r.game.id IN :ids GROUP BY r.game.id")
    List<Object[]> findAvgRatingByGameIds(@Param("ids") List<Long> gameIds);
}

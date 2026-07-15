package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.FavoriteGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteGameRepository extends JpaRepository<FavoriteGame, Long> {

    Optional<FavoriteGame> findByUserIdAndGameId(Long userId, Long gameId);

    @Query("SELECT f.game.id FROM FavoriteGame f WHERE f.user.id = :userId")
    List<Long> findGameIdsByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    long countByGameId(Long gameId);
}

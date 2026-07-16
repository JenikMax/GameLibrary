package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameCollectionRepository extends JpaRepository<GameCollection, Long> {

    List<GameCollection> findByUserIdOrderByUpdatedAtDesc(Long userId);

    List<GameCollection> findByIsPublicTrueOrderByUpdatedAtDesc();

    long countByUserId(Long userId);
}

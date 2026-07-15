package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameCommentRepository extends JpaRepository<GameComment, Long> {

    List<GameComment> findByGameIdOrderByCreatedAtDesc(Long gameId);

    long countByGameId(Long gameId);
}

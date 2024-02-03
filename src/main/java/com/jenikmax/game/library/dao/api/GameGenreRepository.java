package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameGenre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameGenreRepository extends JpaRepository<GameGenre, Long> {
}

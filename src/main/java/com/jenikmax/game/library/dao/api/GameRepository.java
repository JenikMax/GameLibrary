package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;


public interface GameRepository  extends JpaRepository<Game, Long> {
}

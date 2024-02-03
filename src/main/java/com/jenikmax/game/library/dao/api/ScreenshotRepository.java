package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Screenshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenshotRepository extends JpaRepository<Screenshot,Long> {
}

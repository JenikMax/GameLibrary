package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.EmulatorSave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий сейвов браузерного эмулятора.
 */
public interface EmulatorSaveRepository extends JpaRepository<EmulatorSave, Long> {

    /**
     * Находит сейв по игре, пользователю, типу и номеру слота.
     */
    Optional<EmulatorSave> findByGameIdAndUserIdAndKindAndSlot(Long gameId, Long userId, String kind, int slot);

    /**
     * Возвращает все сейвы пользователя для игры (все типы и слоты).
     */
    List<EmulatorSave> findByGameIdAndUserId(Long gameId, Long userId);
}

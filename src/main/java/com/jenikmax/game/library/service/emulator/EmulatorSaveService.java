package com.jenikmax.game.library.service.emulator;

import com.jenikmax.game.library.dao.api.EmulatorSaveRepository;
import com.jenikmax.game.library.model.entity.EmulatorSave;
import com.jenikmax.game.library.model.entity.Game;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Сервис серверного хранения сейвов браузерного эмулятора.
 * Сейвы хранятся per-user (каждый пользователь играет со своими сейвами).
 * Лимиты: srm ≤ 1 МБ, state ≤ 32 МБ.
 */
@Service
public class EmulatorSaveService {

    public static final String KIND_SRM = "srm";
    public static final String KIND_STATE = "state";

    private static final int MAX_SRM_BYTES = 1024 * 1024;
    private static final int MAX_STATE_BYTES = 32 * 1024 * 1024;

    private final EmulatorSaveRepository repository;

    public EmulatorSaveService(EmulatorSaveRepository repository) {
        this.repository = repository;
    }

    /**
     * Находит сейв пользователя для игры.
     * @param gameId ID игры
     * @param userId ID пользователя
     * @param kind тип сейва (srm/state)
     * @param slot номер слота
     */
    public Optional<EmulatorSave> find(Long gameId, Long userId, String kind, int slot) {
        return repository.findByGameIdAndUserIdAndKindAndSlot(gameId, userId, kind, slot);
    }

    /**
     * Возвращает все сейвы пользователя для игры.
     */
    public List<EmulatorSave> list(Long gameId, Long userId) {
        return repository.findByGameIdAndUserId(gameId, userId);
    }

    /**
     * Сохраняет (или обновляет) сейв пользователя для игры.
     * @throws IllegalArgumentException при неверном kind, отрицательном слоте или превышении лимита размера
     */
    @Transactional
    public EmulatorSave save(Long gameId, Long userId, String kind, int slot, String name, byte[] data) {
        if (!KIND_SRM.equals(kind) && !KIND_STATE.equals(kind)) {
            throw new IllegalArgumentException("Invalid save kind: " + kind);
        }
        if (slot < 0) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        int maxBytes = KIND_SRM.equals(kind) ? MAX_SRM_BYTES : MAX_STATE_BYTES;
        if (data.length > maxBytes) {
            throw new IllegalArgumentException("Save is too large: " + data.length + " bytes (max " + maxBytes + ")");
        }

        Optional<EmulatorSave> existing = repository.findByGameIdAndUserIdAndKindAndSlot(gameId, userId, kind, slot);
        EmulatorSave save;
        if (existing.isPresent()) {
            save = existing.get();
        } else {
            save = new EmulatorSave();
            save.setGame(new Game());
            save.getGame().setId(gameId);
            save.setUser(new User());
            save.getUser().setId(userId);
            save.setKind(kind);
            save.setSlot(slot);
        }
        save.setName(name);
        save.setData(data);
        save.setSizeBytes((long) data.length);
        save.setUpdatedAt(new Timestamp(new Date().getTime()));
        return repository.save(save);
    }
}

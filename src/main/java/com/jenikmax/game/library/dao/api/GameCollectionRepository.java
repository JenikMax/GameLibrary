package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для работы с коллекциями игр пользователей.
 * Поддерживает как обычные, так и «умные» коллекции с динамическим составом.
 */
public interface GameCollectionRepository extends JpaRepository<GameCollection, Long> {

    /**
     * Находит все коллекции пользователя, отсортированные по дате обновления (от новых к старым).
     * @param userId ID пользователя
     * @return список коллекций
     */
    List<GameCollection> findByUserIdOrderByUpdatedAtDesc(Long userId);

    /**
     * Находит все публичные коллекции всех пользователей, отсортированные по дате обновления.
     * @return список публичных коллекций
     */
    List<GameCollection> findByIsPublicTrueOrderByUpdatedAtDesc();

    /**
     * Подсчитывает количество коллекций у пользователя.
     * @param userId ID пользователя
     * @return количество коллекций
     */
    long countByUserId(Long userId);
}

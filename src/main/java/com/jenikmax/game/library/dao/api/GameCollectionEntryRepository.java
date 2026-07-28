package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameCollectionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с записями коллекций игр (связь многие-ко-многим).
 * Предоставляет методы для управления играми внутри пользовательских коллекций.
 */
public interface GameCollectionEntryRepository extends JpaRepository<GameCollectionEntry, Long> {

    /**
     * Находит ID коллекций, содержащих указанную игру, принадлежащих указанному пользователю.
     * @param gameId ID игры
     * @param userId ID пользователя
     * @return список ID коллекций
     */
    @org.springframework.data.jpa.repository.Query("SELECT e.collection.id FROM GameCollectionEntry e WHERE e.gameId = :gameId AND e.collection.user.id = :userId")
    List<Long> findCollectionIdsByGameIdAndUserId(@org.springframework.data.repository.query.Param("gameId") Long gameId, @org.springframework.data.repository.query.Param("userId") Long userId);

    /**
     * Находит все записи коллекции, отсортированные по порядку сортировки.
     * @param collectionId ID коллекции
     * @return список записей коллекции
     */
    List<GameCollectionEntry> findByCollectionIdOrderBySortOrderAsc(Long collectionId);

    /**
     * Находит запись коллекции по ID коллекции и ID игры.
     * @param collectionId ID коллекции
     * @param gameId ID игры
     * @return Optional с записью или пустой
     */
    Optional<GameCollectionEntry> findByCollectionIdAndGameId(Long collectionId, Long gameId);

    /**
     * Удаляет игру из коллекции.
     * @param collectionId ID коллекции
     * @param gameId ID игры
     */
    void deleteByCollectionIdAndGameId(Long collectionId, Long gameId);

    /**
     * Подсчитывает количество игр в коллекции.
     * @param collectionId ID коллекции
     * @return количество игр
     */
    long countByCollectionId(Long collectionId);

    /**
     * Возвращает список ID игр, входящих в коллекцию.
     * @param collectionId ID коллекции
     * @return список ID игр
     */
    List<Long> findGameIdByCollectionId(Long collectionId);
}

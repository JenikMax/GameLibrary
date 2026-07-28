package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Репозиторий для работы с тегами игр (таблица game_data_tag).
 */
public interface GameTagRepository extends JpaRepository<GameTag, Long> {

    /**
     * Находит все теги, привязанные к игре.
     * @param gameId ID игры
     * @return список тегов
     */
    List<GameTag> findByGameId(Long gameId);

    /**
     * Возвращает список всех уникальных кодов тегов.
     * @return список кодов тегов
     */
    @Query("SELECT DISTINCT gt.tagCode FROM GameTag gt ORDER BY gt.tagCode")
    List<String> findAllTagCodes();

    /**
     * Удаляет все теги, привязанные к игре.
     * @param gameId ID игры
     */
    void deleteByGameId(Long gameId);
}

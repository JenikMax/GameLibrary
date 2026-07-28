package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.GameComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий для работы с комментариями к играм.
 */
public interface GameCommentRepository extends JpaRepository<GameComment, Long> {

    /**
     * Находит все комментарии к игре, отсортированные по дате создания (от новых к старым).
     * @param gameId ID игры
     * @return список комментариев
     */
    List<GameComment> findByGameIdOrderByCreatedAtDesc(Long gameId);

    /**
     * Подсчитывает количество комментариев у игры.
     * @param gameId ID игры
     * @return количество комментариев
     */
    long countByGameId(Long gameId);

    /**
     * Подсчитывает количество комментариев у пользователя.
     * @param userId ID пользователя
     * @return количество комментариев
     */
    long countByUserId(Long userId);
}

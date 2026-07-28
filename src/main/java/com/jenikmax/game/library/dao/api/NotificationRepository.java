package com.jenikmax.game.library.dao.api;

import com.jenikmax.game.library.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с уведомлениями пользователей.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Находит все уведомления пользователя, отсортированные по дате создания (от новых к старым).
     * @param userId ID пользователя
     * @return список уведомлений
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Находит последние 20 уведомлений пользователя.
     * @param userId ID пользователя
     * @return список уведомлений (макс. 20)
     */
    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Подсчитывает количество непрочитанных уведомлений у пользователя.
     * @param userId ID пользователя
     * @return количество непрочитанных уведомлений
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * Помечает одно уведомление как прочитанное.
     * @param id ID уведомления
     * @param userId ID пользователя (проверка владельца)
     * @return количество обновлённых строк
     */
    @Modifying
    @Query("update Notification n set n.read = true where n.id = :id and n.user.id = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * Помечает все уведомления пользователя как прочитанные.
     * @param userId ID пользователя
     * @return количество обновлённых строк
     */
    @Modifying
    @Query("update Notification n set n.read = true where n.user.id = :userId")
    int markAllAsRead(@Param("userId") Long userId);
}

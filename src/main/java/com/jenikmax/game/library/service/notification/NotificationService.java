package com.jenikmax.game.library.service.notification;

import com.jenikmax.game.library.dao.api.NotificationRepository;
import com.jenikmax.game.library.model.entity.Notification;
import com.jenikmax.game.library.model.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification create(Long userId, String type, String title, String message, Long gameId) {
        Notification n = new Notification();
        n.setUser(new User());
        n.getUser().setId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setGameId(gameId);
        n.setRead(false);
        n.setCreatedAt(new Timestamp(new Date().getTime()));
        return notificationRepository.save(n);
    }

    public List<Notification> getRecent(Long userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        notificationRepository.markAsRead(id, userId);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }
}

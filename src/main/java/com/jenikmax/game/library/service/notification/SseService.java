package com.jenikmax.game.library.service.notification;

import com.jenikmax.game.library.config.jwt.JwtTokenProvider;
import com.jenikmax.game.library.model.dto.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис Server-Sent Events для real-time уведомлений.
 * Управляет подключениями SseEmitter по userId,
 * рассылает события при создании новых уведомлений.
 */
@Service
public class SseService {

    private static final Logger log = LoggerFactory.getLogger(SseService.class);
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Создаёт SSE-подписку для пользователя (таймаут 180с).
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(180_000L);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        try {
            emitter.send(SseEmitter.event().name("connected").data(""));
        } catch (IOException e) {
            remove(userId, emitter);
        }

        return emitter;
    }

    /**
     * Отправляет событие уведомления всем подключениям пользователя.
     */
    public void send(Long userId, NotificationEvent event) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) return;
        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(event));
            } catch (IOException e) {
                remove(userId, emitter);
            }
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) list.remove(emitter);
    }
}

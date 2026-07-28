package com.jenikmax.game.library.service.ai;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Сервис управления фоновыми задачами генерации эмбеддингов.
 * Позволяет запускать полную перегенерацию (с force-сбросом),
 * отслеживать прогресс и автоматически удаляет завершённые задачи через 5 мин.
 */
@Service
public class EmbeddingTaskService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingTaskService.class);

    private final Map<String, EmbeddingTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "embedding-generator");
        t.setDaemon(true);
        return t;
    });

    private final EmbeddingService embeddingService;
    private final JdbcTemplate jdbc;

    @PersistenceUnit
    private EntityManagerFactory emf;

    public EmbeddingTaskService(EmbeddingService embeddingService, JdbcTemplate jdbc) {
        this.embeddingService = embeddingService;
        this.jdbc = jdbc;
    }

    /**
     * Запускает фоновую задачу генерации эмбеддингов для всех игр без эмбеддинга.
     * Если force=true, предварительно сбрасывает все существующие эмбеддинги.
     */
    public String submitGenerateEmbeddings(boolean force) {
        pruneOldTasks();

        String taskId = UUID.randomUUID().toString();
        EmbeddingTask task = new EmbeddingTask(taskId);
        tasks.put(taskId, task);

        executor.submit(() -> {
            try {
                task.setStatus(EmbeddingTask.Status.GENERATING);

                if (force) {
                    jdbc.update("UPDATE library.game_data SET embedding = NULL WHERE embedding IS NOT NULL");
                }

                List<Long> gameIds = jdbc.queryForList(
                        "SELECT id FROM library.game_data " +
                        "WHERE description IS NOT NULL AND description != '' " +
                        "AND embedding IS NULL " +
                        "ORDER BY id",
                        Long.class);

                task.setTotalCount(gameIds.size());
                task.setProgress(0);

                for (int i = 0; i < gameIds.size(); i++) {
                    Long gameId = gameIds.get(i);

                    String name = jdbc.queryForObject(
                            "SELECT name FROM library.game_data WHERE id = ?",
                            String.class, gameId);

                    task.setCurrentGame(name != null ? name : "id=" + gameId);
                    task.setProgress((i * 100) / gameIds.size());

                    embeddingService.generateAndStore(gameId);

                    task.setProcessedCount(i + 1);
                }

                task.setProgress(100);
                task.setStatus(EmbeddingTask.Status.COMPLETED);
                log.info("Embedding generation completed: {} generated, {} total",
                        task.getProcessedCount(), task.getTotalCount());

            } catch (Exception e) {
                log.error("Embedding generation failed", e);
                task.setErrorMessage(e.getMessage());
                task.setStatus(EmbeddingTask.Status.FAILED);
            }
        });

        return taskId;
    }

    /**
     * Возвращает задачу по её идентификатору.
     */
    public EmbeddingTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * Периодически удаляет завершённые задачи старше 5 минут.
     */
    @Scheduled(fixedRate = 60000)
    public void pruneOldTasks() {
        long threshold = System.currentTimeMillis() - 300000;
        Iterator<Map.Entry<String, EmbeddingTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, EmbeddingTask> entry = it.next();
            EmbeddingTask task = entry.getValue();
            if ((task.getStatus() == EmbeddingTask.Status.COMPLETED
                    || task.getStatus() == EmbeddingTask.Status.FAILED)
                    && task.getCreatedAt() < threshold) {
                it.remove();
            }
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}

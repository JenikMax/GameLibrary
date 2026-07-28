package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.service.ai.EmbeddingTask;
import com.jenikmax.game.library.service.ai.EmbeddingTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/embeddings")
/**
 * Контроллер генерации эмбеддингов для семантического поиска.
 * Обрабатывает запросы по пути /api/embeddings.
 * Генерация эмбеддингов (ADMIN only) запускается асинхронно; статус
 * задачи можно отслеживать через polling. Использует AI-сервис для
 * вычисления векторных представлений описаний игр.
 */
public class EmbeddingController {

    private final EmbeddingTaskService taskService;

    public EmbeddingController(EmbeddingTaskService taskService) {
        this.taskService = taskService;
    }

    /**
     * Запустить асинхронную генерацию эмбеддингов для всех игр (ADMIN only).
     * @param force если true — перегенерировать даже существующие эмбеддинги
     * @return 202 Accepted с taskId и URL статуса
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateEmbeddings(
            @RequestParam(defaultValue = "false") boolean force) {
        String taskId = taskService.submitGenerateEmbeddings(force);
        return ResponseEntity.accepted().body(ApiResponse.ok(Map.of(
                "taskId", taskId,
                "statusUrl", "/api/embeddings/status/" + taskId
        )));
    }

    /**
     * Получить статус задачи генерации эмбеддингов.
     * @param taskId идентификатор задачи
     * @return информация о текущем статусе, прогрессе и ошибках
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ApiResponse<EmbeddingTask>> getStatus(@PathVariable String taskId) {
        EmbeddingTask task = taskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.ok(task));
    }
}

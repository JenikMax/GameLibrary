package com.jenikmax.game.library.service.ai;

import com.jenikmax.game.library.config.AiConfig;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Сервис перевода описаний игр между русским и английским языками.
 * Использует AI-клиент для вызова NLLB-200 модели через Python AI-сервис.
 * Автоматически определяет направление перевода по кириллице/латинице.
 * Кэширует результат в колонке description_translated БД.
 * Перевод описаний игр выполняется асинхронно с отслеживанием прогресса
 * по количеству переведённых предложений.
 */
@Service
public class TranslationService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TranslationService.class);
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+(?=[A-ZА-ЯЁ0-9])");

    private final AiClient aiClient;
    private final AiConfig aiConfig;
    private final JdbcTemplate jdbc;

    private final Map<String, TranslateTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "translation-worker");
        t.setDaemon(true);
        return t;
    });

    public TranslationService(AiClient aiClient, AiConfig aiConfig, JdbcTemplate jdbc) {
        this.aiClient = aiClient;
        this.aiConfig = aiConfig;
        this.jdbc = jdbc;
    }

    /**
     * Переводит описание игры и сохраняет результат в БД. Если перевод уже есть — возвращает его.
     */
    public String translateAndCache(Long gameId) {
        if (!isAvailable()) {
            return "";
        }

        String cached = jdbc.queryForObject(
                "SELECT description_translated FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String original = jdbc.queryForObject(
                "SELECT description FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (original == null || original.isEmpty()) {
            return "";
        }

        String cleanText = Jsoup.parse(original).text();
        String direction = detectDirection(cleanText);
        String translated = translateText(cleanText, direction);

        jdbc.update(
                "UPDATE library.game_data SET description_translated = ? WHERE id = ?",
                translated, gameId);

        return translated;
    }

    /**
     * Проверяет доступность AI-сервиса.
     */
    public boolean isAvailable() {
        return aiClient.isAvailable();
    }

    /**
     * Переводит текст в указанном направлении (ru-en или en-ru).
     */
    public String translateText(String text, String direction) {
        if (!isAvailable()) {
            return text;
        }

        try {
            return aiClient.translate(text, direction);
        } catch (Exception e) {
            log.error("Translation failed for direction: {}", direction, e);
            return text;
        }
    }

    /**
     * Переводит произвольный текст с автоопределением направления.
     */
    public String translateArbitraryText(String text) {
        if (!isAvailable() || text == null || text.isBlank()) {
            return text;
        }
        String cleanText = Jsoup.parse(text).text();
        String direction = detectDirection(cleanText);
        return translateText(cleanText, direction);
    }

    /**
     * Локализует название тега: определяет язык, переводит в EN/RU,
     * генерирует нормализованный код из английского текста.
     * Если AI недоступен — все поля заполняются исходным текстом.
     */
    public TagLocalization processTag(String rawInput) {
        String input = rawInput.trim();
        String direction = detectDirection(input);
        boolean isRussian = "ru-en".equals(direction);

        String enText, ruText;

        if (isRussian) {
            ruText = input;
            enText = translateText(input, "ru-en");
        } else {
            enText = input;
            ruText = translateText(input, "en-ru");
        }

        boolean aiAvailable = isAvailable();
        String code;
        if (aiAvailable) {
            code = generateTagCode(enText);
            if (code.isEmpty()) {
                code = "tag_" + Integer.toHexString(input.hashCode());
            }
        } else {
            code = input;
        }

        return new TagLocalization(code,
                aiAvailable ? enText : input,
                aiAvailable ? ruText : input);
    }

    /**
     * Генерирует нормализованный код тега из английского текста:
     * lowercase, пробелы и не-ASCII → подчёркивания, сжатие.
     */
    public static String generateTagCode(String text) {
        if (text == null || text.isBlank()) return "";
        return text.toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    public record TagLocalization(String code, String description, String descriptionRu) {}

    /**
     * Разбивает текст на предложения (regex как в Python AI-сервисе).
     */
    private List<String> splitSentences(String text) {
        text = text.replace('\u00a0', ' ');
        text = text.replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return List.of();
        String[] parts = SENTENCE_SPLIT.split(text);
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result.isEmpty() ? List.of(text) : result;
    }

    /**
     * Запускает асинхронный перевод описания игры с прогрессом по предложениям.
     * Если перевод уже закэширован или описание отсутствует — возвращает готовый результат сразу.
     *
     * @param gameId ID игры
     * @return информация о задаче перевода
     */
    public TranslateTaskInfo translateGameAsync(Long gameId) {
        pruneOldTasks();

        String cached = jdbc.queryForObject(
                "SELECT description_translated FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (cached != null && !cached.isEmpty()) {
            return new TranslateTaskInfo(null, 1, "COMPLETED", cached);
        }

        String original = jdbc.queryForObject(
                "SELECT description FROM library.game_data WHERE id = ?",
                String.class, gameId);
        if (original == null || original.isEmpty()) {
            return new TranslateTaskInfo(null, 0, "COMPLETED", "");
        }

        String cleanText = Jsoup.parse(original).text();
        String direction = detectDirection(cleanText);
        List<String> sentences = splitSentences(cleanText);
        int total = sentences.size();

        String taskId = UUID.randomUUID().toString();
        TranslateTask task = new TranslateTask(taskId);
        task.status = TranslateTask.Status.PENDING;
        task.total.set(total);
        tasks.put(taskId, task);

        log.info("Translation task {} started for game {}: {} sentences",
                taskId, gameId, total);

        executor.submit(() -> doTranslate(task, sentences, direction, gameId));

        return new TranslateTaskInfo(taskId, total, "PENDING", null);
    }

    /**
     * Запускает асинхронный перевод произвольного текста (для страницы редактирования).
     *
     * @param text текст для перевода (может содержать HTML)
     * @return информация о задаче перевода
     */
    public TranslateTaskInfo translateTextAsync(String text) {
        pruneOldTasks();

        if (text == null || text.isBlank()) {
            return new TranslateTaskInfo(null, 0, "COMPLETED", "");
        }

        String cleanText = Jsoup.parse(text).text();
        String direction = detectDirection(cleanText);
        List<String> sentences = splitSentences(cleanText);
        int total = sentences.size();

        String taskId = UUID.randomUUID().toString();
        TranslateTask task = new TranslateTask(taskId);
        task.status = TranslateTask.Status.PENDING;
        task.total.set(total);
        tasks.put(taskId, task);

        log.info("Translation task {} started for arbitrary text: {} sentences",
                taskId, total);

        executor.submit(() -> doTranslate(task, sentences, direction, null));

        return new TranslateTaskInfo(taskId, total, "PENDING", null);
    }

    private void doTranslate(TranslateTask task, List<String> sentences, String direction, Long gameId) {
        try {
            task.status = TranslateTask.Status.RUNNING;
            List<String> translatedParts = new ArrayList<>();
            int total = sentences.size();

            for (int i = 0; i < total; i++) {
                String translated = aiClient.translateSentence(sentences.get(i), direction);
                translatedParts.add(translated);
                task.done.incrementAndGet();
                log.info("Translate progress [{}/{}] sentences for game {}",
                        task.done.get(), total, gameId != null ? gameId : "text");
            }

            String result = String.join(" ", translatedParts);

            if (gameId != null) {
                jdbc.update(
                        "UPDATE library.game_data SET description_translated = ? WHERE id = ?",
                        result, gameId);
            }

            task.result = result;
            task.status = TranslateTask.Status.COMPLETED;
            log.info("Translation completed for game {}: {} sentences", 
                    gameId != null ? gameId : "text", total);
        } catch (Exception e) {
            log.error("Translation task failed for game {}", gameId != null ? gameId : "text", e);
            task.status = TranslateTask.Status.FAILED;
            task.errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error";
        }
    }

    /**
     * Возвращает задачу перевода по ID. Может вернуть null, если задача не найдена
     * или уже удалена при очистке старых задач.
     */
    public TranslateTask getTranslateTask(String taskId) {
        return tasks.get(taskId);
    }

    /**
     * Периодически удаляет завершённые задачи перевода старше 5 минут.
     */
    @Scheduled(fixedRate = 60000)
    public void pruneOldTasks() {
        long now = System.currentTimeMillis();
        long keepMillis = 300000;
        Iterator<Map.Entry<String, TranslateTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TranslateTask> entry = it.next();
            TranslateTask task = entry.getValue();
            if (task.status == TranslateTask.Status.COMPLETED
                    || task.status == TranslateTask.Status.FAILED) {
                if (now - task.createdAt > keepMillis) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }

    /**
     * Модель задачи асинхронного перевода. Отслеживает прогресс по предложениям.
     * Использует AtomicInteger для гарантированной видимости между daemon-потоком и HTTP-потоком.
     */
    public static class TranslateTask {
        public enum Status { PENDING, RUNNING, COMPLETED, FAILED }

        public final String taskId;
        public volatile Status status;
        public final AtomicInteger total = new AtomicInteger(0);
        public final AtomicInteger done = new AtomicInteger(0);
        public volatile String result;
        public volatile String errorMessage;
        public final long createdAt;

        TranslateTask(String taskId) {
            this.taskId = taskId;
            this.status = Status.PENDING;
            this.createdAt = System.currentTimeMillis();
        }
    }

    /**
     * Информация о задаче перевода, возвращаемая при старте.
     * @param taskId ID задачи (null если перевод уже готов)
     * @param totalSentences общее количество предложений
     * @param status статус: PENDING / COMPLETED
     * @param translatedText результат перевода (null во время выполнения)
     */
    public record TranslateTaskInfo(String taskId, int totalSentences, String status, String translatedText) {}

    private String detectDirection(String text) {
        long cyrillicLetters = 0;
        long latinLetters = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CYRILLIC && Character.isLetter(c)) {
                cyrillicLetters++;
            } else if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.BASIC_LATIN && Character.isLetter(c)) {
                latinLetters++;
            }
        }
        long totalLetters = cyrillicLetters + latinLetters;
        if (totalLetters == 0) return "ru-en";
        return (cyrillicLetters > latinLetters) ? "ru-en" : "en-ru";
    }
}

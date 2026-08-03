package com.jenikmax.game.library.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.config.AiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP-клиент для взаимодействия с Python AI-сервисом.
 * Предоставляет методы для проверки доступности, перевода текста
 * и генерации эмбеддингов (одиночных и пакетных).
 * Использует OkHttpClient с таймаутами: обычный 30с, для инференса 60сconnect/300сread.
 */
@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);
    private static final MediaType JSON = MediaType.get("application/json");

    private final OkHttpClient httpClient;
    private final OkHttpClient slowHttpClient;
    private final ObjectMapper objectMapper;
    private final AiConfig aiConfig;

    public AiClient(OkHttpClient httpClient, ObjectMapper objectMapper, AiConfig aiConfig) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.aiConfig = aiConfig;
        this.slowHttpClient = httpClient.newBuilder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Проверяет доступность AI-сервиса через endpoint /health.
     */
    public boolean isAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/health")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                return response.isSuccessful();
            }
        } catch (Exception e) {
            log.debug("AI service not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Отправляет текст на перевод в AI-сервис. Возвращает оригинал при ошибке.
     */
    public String translate(String text, String direction) {
        try {
            String body = objectMapper.writeValueAsString(new TranslateRequest(text, direction));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/translate")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Translation failed: HTTP {}", response.code());
                    return text;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                return json.get("translated").asText();
            }
        } catch (Exception e) {
            log.error("Translation request failed for direction: {}", direction, e);
            return text;
        }
    }

    /**
     * Отправляет одно предложение на перевод. Возвращает оригинал при ошибке.
     */
    public String translateSentence(String text, String direction) {
        try {
            String body = objectMapper.writeValueAsString(new TranslateSentenceRequest(text, direction));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/translate/sentence")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Sentence translation failed: HTTP {}", response.code());
                    return text;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                return json.get("translated").asText();
            }
        } catch (Exception e) {
            log.error("Sentence translation request failed", e);
            return text;
        }
    }

    /**
     * Генерирует эмбеддинг для одного текста через AI-сервис.
     */
    public float[] embed(String text) {
        try {
            String body = objectMapper.writeValueAsString(new EmbedRequest(text));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/embed")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Embedding failed: HTTP {}", response.code());
                    return null;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                return parseFloatArray(json.get("embedding"));
            }
        } catch (Exception e) {
            log.error("Embedding request failed", e);
            return null;
        }
    }

    /**
     * Генерирует эмбеддинги для списка текстов (пакетный режим).
     */
    public float[][] embedBatch(List<String> texts) {
        try {
            String body = objectMapper.writeValueAsString(new EmbedBatchRequest(texts));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/embed/batch")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Batch embedding failed: HTTP {}", response.code());
                    return null;
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                JsonNode embeddings = json.get("embeddings");
                float[][] result = new float[embeddings.size()][];
                for (int i = 0; i < embeddings.size(); i++) {
                    result[i] = parseFloatArray(embeddings.get(i));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Batch embedding request failed", e);
            return null;
        }
    }

    private float[] parseFloatArray(JsonNode node) {
        float[] result = new float[node.size()];
        for (int i = 0; i < node.size(); i++) {
            result[i] = (float) node.get(i).asDouble();
        }
        return result;
    }

    /**
     * Проверяет доступность vision-модели (CLIP) через /health.
     */
    public boolean isVisionAvailable() {
        try {
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/health")
                    .get()
                    .build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) return false;
                JsonNode json = objectMapper.readTree(response.body().string());
                JsonNode models = json.get("models");
                return models != null && "loaded".equals(models.get("clip").asText(""));
            }
        } catch (Exception e) {
            log.debug("Vision model not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Классификация одного скриншота по текстовым меткам через CLIP.
     */
    public List<LabelMatch> classifyImage(byte[] imageBytes, List<String> labels, int topK) {
        try {
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            String body = objectMapper.writeValueAsString(
                    new VisionClassifyRequest(imageBase64, labels, topK));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/vision/classify")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Vision classify failed: HTTP {}", response.code());
                    return List.of();
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                List<LabelMatch> result = new ArrayList<>();
                for (JsonNode match : json.get("matches")) {
                    result.add(new LabelMatch(match.get("label").asText(), (float) match.get("score").asDouble()));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Vision classify request failed", e);
            return List.of();
        }
    }

    /**
     * Классификация нескольких скриншотов с агрегацией через CLIP.
     */
    public List<LabelMatch> classifyImagesMulti(List<byte[]> imagesBytes, List<String> labels, int topK) {
        try {
            List<String> imagesBase64 = new ArrayList<>();
            for (byte[] img : imagesBytes) {
                imagesBase64.add(Base64.getEncoder().encodeToString(img));
            }
            String body = objectMapper.writeValueAsString(
                    new VisionClassifyMultiRequest(imagesBase64, labels, topK));
            Request request = new Request.Builder()
                    .url(aiConfig.getServiceUrl() + "/vision/classify-multi")
                    .post(RequestBody.create(JSON, body))
                    .build();
            try (Response response = slowHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Vision multi-classify failed: HTTP {}", response.code());
                    return List.of();
                }
                JsonNode json = objectMapper.readTree(response.body().string());
                List<LabelMatch> result = new ArrayList<>();
                for (JsonNode match : json.get("matches")) {
                    result.add(new LabelMatch(match.get("label").asText(), (float) match.get("score").asDouble()));
                }
                return result;
            }
        } catch (Exception e) {
            log.error("Vision multi-classify request failed", e);
            return List.of();
        }
    }

    public record LabelMatch(String label, float score) {}

    private record TranslateRequest(String text, String direction) {}
    private record TranslateSentenceRequest(String text, String direction) {}
    private record EmbedRequest(String text) {}
    private record EmbedBatchRequest(List<String> texts) {}
    private record VisionClassifyRequest(String image_base64, List<String> labels, int top_k) {}
    private record VisionClassifyMultiRequest(List<String> images_base64, List<String> labels, int top_k) {}
}

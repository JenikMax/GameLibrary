package com.jenikmax.game.library.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jenikmax.game.library.config.AiConfig;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

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

    private record TranslateRequest(String text, String direction) {}
    private record EmbedRequest(String text) {}
    private record EmbedBatchRequest(List<String> texts) {}
}

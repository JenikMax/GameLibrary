package com.jenikmax.game.library.service.ai;

import ai.onnxruntime.*;
import com.jenikmax.game.library.config.AiConfig;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnnxModelManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OnnxModelManager.class);

    private final OrtEnvironment env;
    private final AiConfig aiConfig;
    private final Path modelsDir;
    private final Map<String, OrtSession> sessions = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public OnnxModelManager(AiConfig aiConfig) {
        this.aiConfig = aiConfig;
        this.env = OrtEnvironment.getEnvironment();
        this.modelsDir = Path.of(aiConfig.getModelsDir());
    }

    public synchronized OrtSession getSession(String modelFile) {
        if (closed) throw new IllegalStateException("OnnxModelManager is closed");
        Path modelPath = modelsDir.resolve(modelFile);
        if (!Files.exists(modelPath)) {
            log.warn("ONNX model not found: {}", modelPath);
            return null;
        }
        return sessions.computeIfAbsent(modelFile, key -> {
            try {
                log.info("Loading ONNX model: {}", modelPath);
                return env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
            } catch (OrtException e) {
                throw new RuntimeException("Failed to load ONNX model: " + key, e);
            }
        });
    }

    public float[] generateEmbedding(SentencePieceTokenizer tokenizer, String text) {
        AiConfig.Embedding embConfig = aiConfig.getEmbedding();
        long[] inputIds = tokenizer.encode(text, embConfig.getMaxLength());
        long[] attentionMask = createAttentionMask(inputIds, tokenizer.getPadTokenId());

        OrtSession session = getSession(embConfig.getModelFile());
        if (session == null) return null;

        try (var inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds),
                    new long[]{1, inputIds.length});
             var maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask),
                    new long[]{1, attentionMask.length})) {

            Map<String, OnnxTensor> inputs = Map.of(
                    "input_ids", inputIdsTensor,
                    "attention_mask", maskTensor
            );

            try (var result = session.run(inputs)) {
                return extractEmbedding(result, attentionMask, embConfig.getDimension());
            }
        } catch (OrtException e) {
            throw new RuntimeException("Embedding inference failed", e);
        }
    }

    private float[] extractEmbedding(OrtSession.Result result, long[] attentionMask, int dimension) throws OrtException {
        Object rawOutput = result.get(0).getValue();

        float[][] pooled;
        if (rawOutput instanceof float[][][]) {
            float[][][] hidden = (float[][][]) rawOutput;
            pooled = meanPool(hidden, attentionMask);
        } else if (rawOutput instanceof float[][]) {
            pooled = (float[][]) rawOutput;
        } else {
            throw new RuntimeException("Unexpected embedding output shape: " + rawOutput.getClass());
        }

        float[] embedding = new float[dimension];
        for (int i = 0; i < dimension && i < pooled[0].length; i++) {
            embedding[i] = pooled[0][i];
        }
        return normalize(embedding);
    }

    private float[][] meanPool(float[][][] hidden, long[] mask) {
        int batch = hidden.length;
        int seqLen = hidden[0].length;
        int dim = hidden[0][0].length;

        float[][] pooled = new float[batch][dim];
        for (int b = 0; b < batch; b++) {
            int valid = 0;
            for (int i = 0; i < seqLen; i++) {
                if (mask[i] == 0) continue;
                for (int d = 0; d < dim; d++) {
                    pooled[b][d] += hidden[b][i][d];
                }
                valid++;
            }
            if (valid > 0) {
                for (int d = 0; d < dim; d++) {
                    pooled[b][d] /= valid;
                }
            }
        }
        return pooled;
    }

    private float[] normalize(float[] v) {
        float norm = 0;
        for (float x : v) norm += x * x;
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
        return v;
    }

    public String translate(SentencePieceTokenizer tokenizer, String text,
                            String modelFile, String vocabFile, int maxLength) {
        if (tokenizer == null) return text;
        long[] inputIds = tokenizer.encode(text, maxLength);
        long[] attentionMask = createAttentionMask(inputIds, tokenizer.getPadTokenId());

        OrtSession session = getSession(modelFile);
        if (session == null) return text;

        try (var inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds),
                    new long[]{1, inputIds.length});
             var maskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask),
                    new long[]{1, attentionMask.length})) {

            Map<String, OnnxTensor> inputs = Map.of(
                    "input_ids", inputTensor,
                    "attention_mask", maskTensor
            );

            try (var result = session.run(inputs)) {
                float[][][] logits = (float[][][]) result.get(0).getValue();
                return greedyDecode(tokenizer, logits[0]);
            }
        } catch (OrtException e) {
            throw new RuntimeException("Translation inference failed", e);
        }
    }

    private String greedyDecode(SentencePieceTokenizer tokenizer, float[][] tokenLogits) {
        List<Long> outputIds = new ArrayList<>();
        int bosId = tokenizer.getBosTokenId();
        int eosId = tokenizer.getEosTokenId();

        for (float[] logit : tokenLogits) {
            int bestId = argmax(logit);
            if (bestId == eosId || bestId == bosId || bestId == tokenizer.getPadTokenId()) {
                if (outputIds.isEmpty()) continue;
                break;
            }
            outputIds.add((long) bestId);
        }

        long[] ids = new long[outputIds.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = outputIds.get(i);
        return tokenizer.decode(ids);
    }

    private int argmax(float[] array) {
        int best = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[best]) best = i;
        }
        return best;
    }

    private long[] createAttentionMask(long[] inputIds, long padId) {
        long[] mask = new long[inputIds.length];
        for (int i = 0; i < inputIds.length; i++) {
            mask[i] = (inputIds[i] != padId) ? 1 : 0;
        }
        return mask;
    }

    @Override
    @PreDestroy
    public void close() {
        closed = true;
        sessions.forEach((name, session) -> {
            try {
                session.close();
                log.info("Closed ONNX session: {}", name);
            } catch (Exception e) {
                log.warn("Failed to close ONNX session: {}", name, e);
            }
        });
        sessions.clear();
        try {
            env.close();
        } catch (Exception e) {
            log.warn("Failed to close ONNX environment", e);
        }
    }
}

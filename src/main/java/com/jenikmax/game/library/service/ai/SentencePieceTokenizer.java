package com.jenikmax.game.library.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;

public class SentencePieceTokenizer {

    private static final int PAD_ID = 0;
    private static final int UNK_ID = 3;
    private static final int BOS_ID = 1;
    private static final int EOS_ID = 2;

    private final Map<String, Integer> vocab;
    private final List<String> idToToken;
    private final Map<Pair, Integer> merges;
    private final int padId;
    private final int unkId;
    private final int bosId;
    private final int eosId;

    public SentencePieceTokenizer(String vocabPath) throws IOException {
        this(loadTokenJson(vocabPath));
    }

    private SentencePieceTokenizer(JsonNode root) {
        JsonNode model = root.get("model");
        JsonNode vocabNode = model.get("vocab");

        this.vocab = new HashMap<>();
        int maxId = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = vocabNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            int id = entry.getValue().asInt();
            vocab.put(entry.getKey(), id);
            maxId = Math.max(maxId, id);
        }

        this.idToToken = new ArrayList<>(maxId + 1);
        for (int i = 0; i <= maxId; i++) {
            idToToken.add(null);
        }
        for (Map.Entry<String, Integer> entry : vocab.entrySet()) {
            idToToken.set(entry.getValue(), entry.getKey());
        }

        this.merges = new HashMap<>();
        JsonNode mergesNode = model.get("merges");
        if (mergesNode != null && mergesNode.isArray()) {
            for (int i = 0; i < mergesNode.size(); i++) {
                String merge = mergesNode.get(i).asText();
                String[] parts = merge.split(" ");
                if (parts.length == 2) {
                    merges.put(new Pair(parts[0], parts[1]), i);
                }
            }
        }

        this.padId = getTokenId("<pad>", PAD_ID);
        this.bosId = getTokenId("<s>", getTokenId("<bos>", BOS_ID));
        this.eosId = getTokenId("</s>", getTokenId("<eos>", EOS_ID));
        this.unkId = getTokenId("<unk>", UNK_ID);
    }

    private static JsonNode loadTokenJson(String path) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream is = SentencePieceTokenizer.class.getClassLoader().getResourceAsStream(path);
        if (is == null) {
            is = new java.io.FileInputStream(path);
        }
        return mapper.readTree(is);
    }

    private int getTokenId(String token, int defaultId) {
        Integer id = vocab.get(token);
        return id != null ? id : defaultId;
    }

    public long[] encode(String text) {
        return encode(text, 0);
    }

    public long[] encode(String text, int maxLength) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase();
        List<Long> ids = new ArrayList<>();

        if (bosId > 0) {
            ids.add((long) bosId);
        }

        String[] words = splitIntoWords(normalized);
        for (String word : words) {
            if (word.isEmpty()) continue;

            List<String> tokens = applyBPE(word);
            for (String token : tokens) {
                Integer id = vocab.get(token);
                if (id == null && token.startsWith("\u2581")) {
                    id = vocab.get(token.substring(1));
                }
                ids.add(id != null ? (long) id : (long) unkId);
            }
        }

        if (eosId > 0) {
            ids.add((long) eosId);
        }

        if (maxLength > 0 && ids.size() < maxLength) {
            while (ids.size() < maxLength) {
                ids.add((long) padId);
            }
        }
        if (maxLength > 0 && ids.size() > maxLength) {
            ids = ids.subList(0, maxLength);
        }

        long[] result = new long[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    public String decode(long[] ids) {
        StringBuilder sb = new StringBuilder();
        for (long id : ids) {
            if (id == padId || id == bosId || id == eosId) continue;
            if (id == unkId) continue;
            if (id >= 0 && id < idToToken.size()) {
                String token = idToToken.get((int) id);
                if (token != null) {
                    sb.append(token.replace("\u2581", " "));
                }
            }
        }
        return sb.toString().trim();
    }

    private List<String> applyBPE(String word) {
        List<String> chars = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            int codePoint = word.codePointAt(i);
            String c = new String(Character.toChars(codePoint));
            if (i == 0) {
                c = "\u2581" + c;
            }
            chars.add(c);
            if (Character.isSupplementaryCodePoint(codePoint)) {
                i++;
            }
        }

        while (true) {
            int bestRank = Integer.MAX_VALUE;
            int bestIdx = -1;

            for (int i = 0; i < chars.size() - 1; i++) {
                Pair pair = new Pair(chars.get(i), chars.get(i + 1));
                Integer rank = merges.get(pair);
                if (rank != null && rank < bestRank) {
                    bestRank = rank;
                    bestIdx = i;
                }
            }

            if (bestIdx < 0) break;

            chars.set(bestIdx, chars.get(bestIdx) + chars.get(bestIdx + 1));
            chars.remove(bestIdx + 1);
        }

        return chars;
    }

    private String[] splitIntoWords(String text) {
        List<String> words = new ArrayList<>();
        for (int i = 0; i < text.length(); i++) {
            int codePoint = text.codePointAt(i);
            int type = Character.getType(codePoint);

            if (Character.isWhitespace(codePoint)) {
                continue;
            }

            StringBuilder word = new StringBuilder();
            word.appendCodePoint(codePoint);
            if (Character.isSupplementaryCodePoint(codePoint)) i++;

            while (i + 1 < text.length()) {
                int nextCp = text.codePointAt(i + 1);
                int nextType = Character.getType(nextCp);
                boolean nextIsPunct = isPunctuation(nextCp);

                if (isPunctuation(codePoint) != nextIsPunct || Character.isWhitespace(nextCp)) {
                    break;
                }

                word.appendCodePoint(nextCp);
                i++;
                if (Character.isSupplementaryCodePoint(nextCp)) i++;
                codePoint = nextCp;
            }

            words.add(word.toString());
        }
        return words.toArray(new String[0]);
    }

    private boolean isPunctuation(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.DASH_PUNCTUATION
            || type == Character.START_PUNCTUATION
            || type == Character.END_PUNCTUATION
            || type == Character.CONNECTOR_PUNCTUATION
            || type == Character.OTHER_PUNCTUATION
            || type == Character.INITIAL_QUOTE_PUNCTUATION
            || type == Character.FINAL_QUOTE_PUNCTUATION;
    }

    public int getPadTokenId() { return padId; }
    public int getUnkTokenId() { return unkId; }
    public int getBosTokenId() { return bosId; }
    public int getEosTokenId() { return eosId; }

    private static class Pair {
        private final String first;
        private final String second;

        Pair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;
            Pair pair = (Pair) o;
            return first.equals(pair.first) && second.equals(pair.second);
        }

        @Override
        public int hashCode() {
            return 31 * first.hashCode() + second.hashCode();
        }
    }
}

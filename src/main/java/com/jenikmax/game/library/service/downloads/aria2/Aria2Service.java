package com.jenikmax.game.library.service.downloads.aria2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class Aria2Service {

    private static final Logger logger = LoggerFactory.getLogger(Aria2Service.class);
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;
    private final String rpcUrl;
    private final String rpcSecret;
    private final String gamesDir;
    private final String aria2DownloadDir;
    private int requestId = 0;

    public Aria2Service(
            @Value("${game-library.aria2.rpc-url:http://aria2:6800/jsonrpc}") String rpcUrl,
            @Value("${game-library.aria2.rpc-secret:}") String rpcSecret,
            @Value("${game-library.games.directory:/gameLibrary}") String gamesDir,
            @Value("${game-library.aria2.download-dir:/downloads}") String aria2DownloadDir) {
        this.rpcUrl = rpcUrl;
        this.rpcSecret = rpcSecret;
        this.gamesDir = gamesDir;
        this.aria2DownloadDir = aria2DownloadDir;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Add a torrent file to aria2 for seeding.
     * @param torrentPath absolute path to .torrent file
     * @param dir download directory (where the game files are)
     * @return GID if successful, null otherwise
     */
    public String addTorrent(String torrentPath, String dir) {
        try {
            byte[] torrentBytes = Files.readAllBytes(new File(torrentPath).toPath());
            String base64 = Base64.getEncoder().encodeToString(torrentBytes);
            String mappedDir = dir != null ? dir.replace(gamesDir, aria2DownloadDir) : null;
            logger.info("addTorrent: dir={}, mappedDir={}, base64Len={}",
                    dir, mappedDir, base64.length());

            ArrayNode params = mapper.createArrayNode();
            params.add("token:" + rpcSecret);
            params.add(base64);
            params.add(mapper.createArrayNode());
            ObjectNode options = mapper.createObjectNode();
            options.put("dir", mappedDir);
            params.add(options);

            ObjectNode request = mapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", ++requestId);
            request.put("method", "aria2.addTorrent");
            request.set("params", params);

            String json = mapper.writeValueAsString(request);
            logger.info("addTorrent JSON: {}", json);

            RequestBody body = RequestBody.create(JSON, json);
            Request httpRequest = new Request.Builder()
                    .url(rpcUrl)
                    .post(body)
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : null;
                logger.info("addTorrent RESPONSE ({}): {}", httpResponse.code(), responseBody);
                if (!httpResponse.isSuccessful()) return null;
                Map<String, Object> response = mapper.readValue(responseBody, Map.class);
                if (response != null && response.containsKey("result")) {
                    String gid = (String) response.get("result");
                    logger.info("aria2 addTorrent success, GID: {}", gid);
                    return gid;
                }
                if (response != null && response.containsKey("error")) {
                    logger.warn("aria2 addTorrent RPC error: {}", response.get("error"));
                }
            }
        } catch (Exception e) {
            logger.error("aria2 addTorrent failed", e);
        }
        return null;
    }

    private boolean changeDownloadOption(String gid, String key, String value) {
        try {
            ArrayNode params = mapper.createArrayNode();
            params.add("token:" + rpcSecret);
            params.add(gid);
            ObjectNode options = mapper.createObjectNode();
            options.put(key, value);
            params.add(options);

            ObjectNode request = mapper.createObjectNode();
            request.put("jsonrpc", "2.0");
            request.put("id", ++requestId);
            request.put("method", "aria2.changeOption");
            request.set("params", params);

            String json = mapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(JSON, json);
            Request httpRequest = new Request.Builder()
                    .url(rpcUrl)
                    .post(body)
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : null;
                if (!httpResponse.isSuccessful()) {
                    logger.warn("aria2.changeOption failed for {}={}: {}", key, value, responseBody);
                    return false;
                }
                Map<String, Object> resp = mapper.readValue(responseBody, Map.class);
                return resp != null && resp.containsKey("result");
            }
        } catch (Exception e) {
            logger.warn("aria2.changeOption failed for {}={}: {}", key, value, e.getMessage());
            return false;
        }
    }

    /**
     * Add a direct HTTP/HTTPS URI download.
     */
    public String addUri(String uri, String dir) {
        try {
            List<Object> uris = Collections.singletonList(uri);
            Map<String, Object> options = new HashMap<>();
            if (dir != null) options.put("dir", dir);
            List<Object> params = buildParams(uris, options);
            Map<String, Object> response = callRpc("aria2.addUri", params);
            if (response != null && response.containsKey("result")) {
                String gid = (String) response.get("result");
                logger.info("aria2 addUri success, GID: {}", gid);
                return gid;
            }
        } catch (Exception e) {
            logger.error("aria2 addUri failed", e);
        }
        return null;
    }

    /**
     * Get status of a download by GID.
     */
    public Map<String, Object> tellStatus(String gid) {
        try {
            List<Object> params = buildParams(gid);
            params.add(createFields("gid", "status", "totalLength", "completedLength",
                    "downloadSpeed", "uploadSpeed", "dir", "files", "bittorrent"));
            Map<String, Object> response = callRpc("aria2.tellStatus", params);
            if (response != null && response.containsKey("result")) {
                return (Map<String, Object>) response.get("result");
            }
        } catch (Exception e) {
            logger.warn("aria2 tellStatus failed: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * List all active downloads.
     */
    public List<Map<String, Object>> tellActive() {
        try {
            List<Object> params = buildParams();
            Map<String, Object> response = callRpc("aria2.tellActive", params);
            if (response != null && response.containsKey("result")) {
                return (List<Map<String, Object>>) response.get("result");
            }
        } catch (Exception e) {
            logger.warn("aria2 tellActive failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * List waiting downloads.
     */
    public List<Map<String, Object>> tellWaiting(int offset, int num) {
        try {
            List<Object> params = buildParams(offset, num);
            Map<String, Object> response = callRpc("aria2.tellWaiting", params);
            if (response != null && response.containsKey("result")) {
                return (List<Map<String, Object>>) response.get("result");
            }
        } catch (Exception e) {
            logger.warn("aria2 tellWaiting failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * List stopped downloads.
     */
    public List<Map<String, Object>> tellStopped(int offset, int num) {
        try {
            List<Object> params = buildParams(offset, num);
            Map<String, Object> response = callRpc("aria2.tellStopped", params);
            if (response != null && response.containsKey("result")) {
                return (List<Map<String, Object>>) response.get("result");
            }
        } catch (Exception e) {
            logger.warn("aria2 tellStopped failed: {}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * Remove a download by GID.
     */
    public boolean remove(String gid) {
        try {
            List<Object> params = buildParams(gid);
            Map<String, Object> response = callRpc("aria2.remove", params);
            return response != null && response.containsKey("result");
        } catch (Exception e) {
            logger.warn("aria2 remove failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Pause a download.
     */
    public boolean pause(String gid) {
        try {
            List<Object> params = buildParams(gid);
            Map<String, Object> response = callRpc("aria2.pause", params);
            return response != null && response.containsKey("result");
        } catch (Exception e) {
            logger.warn("aria2 pause failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Unpause a download.
     */
    public boolean unpause(String gid) {
        try {
            List<Object> params = buildParams(gid);
            Map<String, Object> response = callRpc("aria2.unpause", params);
            return response != null && response.containsKey("result");
        } catch (Exception e) {
            logger.warn("aria2 unpause failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get global statistics (overall speed, active downloads count, etc.)
     */
    public Map<String, Object> getGlobalStat() {
        try {
            List<Object> params = buildParams();
            Map<String, Object> response = callRpc("aria2.getGlobalStat", params);
            if (response != null && response.containsKey("result")) {
                return (Map<String, Object>) response.get("result");
            }
        } catch (Exception e) {
            logger.warn("aria2 getGlobalStat failed: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    /**
     * Get version info.
     */
    public boolean isConnected() {
        try {
            List<Object> params = buildParams();
            Map<String, Object> response = callRpc("aria2.getVersion", params);
            return response != null && response.containsKey("result");
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callRpc(String method, List<Object> params) {
        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", ++requestId);
            request.put("method", method);
            request.put("params", params);

            String json = mapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(JSON, json);
            logger.info("RPC request {}: len={}, body={}", method, json.length(), json.length() > 300 ? json.substring(0, 300) + "..." : json);

            Request httpRequest = new Request.Builder()
                    .url(rpcUrl)
                    .post(body)
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : null;
                if (!httpResponse.isSuccessful()) {
                    logger.warn("aria2 RPC call {} returned {}: body={}", method, httpResponse.code(), responseBody);
                    return null;
                }
                if (responseBody == null) return null;
                return mapper.readValue(responseBody, Map.class);
            }
        } catch (IOException e) {
            logger.warn("aria2 RPC call {} failed: {}", method, e.getMessage());
            return null;
        }
    }

    private List<Object> buildParams(Object... args) {
        List<Object> params = new ArrayList<>();
        if (rpcSecret != null && !rpcSecret.isEmpty()) {
            params.add("token:" + rpcSecret);
        }
        if (args != null) {
            Collections.addAll(params, args);
        }
        return params;
    }

    private static List<String> createFields(String... fields) {
        return Arrays.asList(fields);
    }
}

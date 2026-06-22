package com.jenikmax.game.library.service.downloads.transmission;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TransmissionService {

    private static final Logger logger = LoggerFactory.getLogger(TransmissionService.class);
    private static final MediaType JSON = MediaType.parse("application/json");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OkHttpClient client;
    private final String rpcUrl;
    private final String gamesDir;
    private final String transmissionDownloadDir;

    public TransmissionService(
            @Value("${game-library.transmission.rpc-url:http://transmission:9091/transmission/rpc}") String rpcUrl,
            @Value("${game-library.games.directory:/gameLibrary}") String gamesDir,
            @Value("${game-library.transmission.download-dir:/downloads}") String transmissionDownloadDir) {
        this.rpcUrl = rpcUrl;
        this.gamesDir = gamesDir;
        this.transmissionDownloadDir = transmissionDownloadDir;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private String getSessionId() {
        try {
            Request request = new Request.Builder()
                    .url(rpcUrl)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.header("X-Transmission-Session-Id");
            }
        } catch (IOException e) {
            logger.warn("Failed to get Transmission session id: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callRpc(String method, Map<String, Object> arguments) {
        String sessionId = getSessionId();
        if (sessionId == null) return null;

        try {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("method", method);
            request.put("arguments", arguments != null ? arguments : new LinkedHashMap<>());

            String json = mapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(JSON, json);
            Request httpRequest = new Request.Builder()
                    .url(rpcUrl)
                    .header("X-Transmission-Session-Id", sessionId)
                    .post(body)
                    .build();

            try (Response httpResponse = client.newCall(httpRequest).execute()) {
                String responseBody = httpResponse.body() != null ? httpResponse.body().string() : null;
                if (!httpResponse.isSuccessful()) {
                    logger.warn("Transmission RPC call {} returned {}: {}", method, httpResponse.code(), responseBody);
                    return null;
                }
                if (responseBody == null) return null;
                return mapper.readValue(responseBody, Map.class);
            }
        } catch (IOException e) {
            logger.warn("Transmission RPC call {} failed: {}", method, e.getMessage());
            return null;
        }
    }

    public String addTorrent(String torrentPath, String dir) {
        try {
            byte[] torrentBytes = Files.readAllBytes(new File(torrentPath).toPath());
            String base64 = Base64.getEncoder().encodeToString(torrentBytes);

            String mappedDir = mapDownloadDir(dir);
            logger.info("addTorrent: dir={}, mappedDir={}", dir, mappedDir);

            Map<String, Object> args = new LinkedHashMap<>();
            args.put("metainfo", base64);
            args.put("download-dir", mappedDir);
            args.put("paused", false);

            Map<String, Object> response = callRpc("torrent-add", args);
            if (response != null && response.containsKey("arguments")) {
                Map<String, Object> argsResp = (Map<String, Object>) response.get("arguments");
                if (argsResp.containsKey("torrent-added")) {
                    Map<String, Object> added = (Map<String, Object>) argsResp.get("torrent-added");
                    Object id = added.get("id");
                    String idStr = String.valueOf(id);
                    logger.info("Transmission addTorrent success, ID: {}", idStr);
                    return idStr;
                }
                if (argsResp.containsKey("torrent-duplicate")) {
                    Map<String, Object> dup = (Map<String, Object>) argsResp.get("torrent-duplicate");
                    Object id = dup.get("id");
                    logger.info("Torrent already added in Transmission, ID: {}", id);
                    return String.valueOf(id);
                }
            }
            if (response != null && response.containsKey("result")) {
                logger.warn("Transmission addTorrent error: {}", response.get("result"));
            }
        } catch (Exception e) {
            logger.error("Transmission addTorrent failed", e);
        }
        return null;
    }

    public Map<String, Object> getStatus(String id) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ids", Collections.singletonList(parseId(id)));
        args.put("fields", Arrays.asList(
                "id", "name", "status", "totalSize", "downloadedEver", "uploadedEver",
                "rateDownload", "rateUpload", "percentDone", "error", "errorString"));

        Map<String, Object> response = callRpc("torrent-get", args);
        if (response != null && response.containsKey("arguments")) {
            Map<String, Object> argsResp = (Map<String, Object>) response.get("arguments");
            if (argsResp.containsKey("torrents")) {
                List<Map<String, Object>> torrents = (List<Map<String, Object>>) argsResp.get("torrents");
                if (!torrents.isEmpty()) {
                    return simplifyStatus(torrents.get(0));
                }
            }
        }
        return Collections.emptyMap();
    }

    public List<Map<String, Object>> getActive() {
        return getTorrentsByStatus(Arrays.asList(4, 6));
    }

    public List<Map<String, Object>> getWaiting() {
        return getTorrentsByStatus(Arrays.asList(1, 2, 3, 5));
    }

    public List<Map<String, Object>> getStopped(int offset, int limit) {
        return getTorrentsByStatus(Collections.singletonList(0));
    }

    private List<Map<String, Object>> getTorrentsByStatus(List<Integer> statuses) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("fields", Arrays.asList(
                "id", "name", "status", "totalSize", "downloadedEver", "uploadedEver",
                "rateDownload", "rateUpload", "percentDone", "error", "errorString"));

        Map<String, Object> response = callRpc("torrent-get", args);
        if (response != null && response.containsKey("arguments")) {
            Map<String, Object> argsResp = (Map<String, Object>) response.get("arguments");
            if (argsResp.containsKey("torrents")) {
                List<Map<String, Object>> all = (List<Map<String, Object>>) argsResp.get("torrents");
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> t : all) {
                    int status = t.get("status") instanceof Number ? ((Number) t.get("status")).intValue() : -1;
                    if (statuses.contains(status)) {
                        result.add(simplifyStatus(t));
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }

    public boolean remove(String id, boolean deleteFiles) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ids", Collections.singletonList(parseId(id)));
        args.put("delete-local-data", deleteFiles);
        Map<String, Object> response = callRpc("torrent-remove", args);
        return response != null && "success".equals(response.get("result"));
    }

    public boolean stopTorrent(String id) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ids", Collections.singletonList(parseId(id)));
        Map<String, Object> response = callRpc("torrent-stop", args);
        return response != null && "success".equals(response.get("result"));
    }

    public boolean startTorrent(String id) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("ids", Collections.singletonList(parseId(id)));
        Map<String, Object> response = callRpc("torrent-start", args);
        return response != null && "success".equals(response.get("result"));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getGlobalStat() {
        Map<String, Object> response = callRpc("session-stats", null);
        if (response != null && response.containsKey("arguments")) {
            return (Map<String, Object>) response.get("arguments");
        }
        return Collections.emptyMap();
    }

    public boolean isConnected() {
        Map<String, Object> response = callRpc("session-get", null);
        return response != null && "success".equals(response.get("result"));
    }

    private Map<String, Object> simplifyStatus(Map<String, Object> raw) {
        Map<String, Object> simple = new LinkedHashMap<>();
        simple.put("gid", String.valueOf(raw.get("id")));
        simple.put("name", raw.get("name"));

        int ts = raw.get("status") instanceof Number ? ((Number) raw.get("status")).intValue() : 0;
        simple.put("status", mapTransmissionStatus(ts));

        long total = raw.get("totalSize") instanceof Number ? ((Number) raw.get("totalSize")).longValue() : 0;
        long downloaded = raw.get("downloadedEver") instanceof Number ? ((Number) raw.get("downloadedEver")).longValue() : 0;
        simple.put("totalLength", total);
        simple.put("completedLength", downloaded);

        long dlSpeed = raw.get("rateDownload") instanceof Number ? ((Number) raw.get("rateDownload")).longValue() : 0;
        long ulSpeed = raw.get("rateUpload") instanceof Number ? ((Number) raw.get("rateUpload")).longValue() : 0;
        simple.put("downloadSpeed", dlSpeed);
        simple.put("uploadSpeed", ulSpeed);

        double pct = raw.get("percentDone") instanceof Number ? ((Number) raw.get("percentDone")).doubleValue() * 100.0 : 0.0;
        simple.put("progress", Math.round(pct * 100.0) / 100.0);

        return simple;
    }

    private String mapTransmissionStatus(int ts) {
        switch (ts) {
            case 0:  return "stopped";
            case 1:  return "waiting";
            case 2:  return "active";
            case 3:  return "waiting";
            case 4:  return "active";
            case 5:  return "waiting";
            case 6:  return "active";
            default: return "unknown";
        }
    }

    private int parseId(String id) {
        try { return Integer.parseInt(id); }
        catch (NumberFormatException e) { return 0; }
    }

    private String mapDownloadDir(String dir) {
        if (dir == null) return transmissionDownloadDir;
        if (dir.endsWith("/")) dir = dir.substring(0, dir.length() - 1);
        String parent = dir.contains("/") ? dir.substring(0, dir.lastIndexOf("/")) : dir;
        String prefix = gamesDir + "/games";
        String relative;
        if (parent.startsWith(prefix)) {
            relative = parent.substring(gamesDir.length());
        } else if (parent.startsWith(gamesDir)) {
            relative = parent.substring(gamesDir.length());
        } else {
            relative = "/" + dir;
        }
        if (relative.startsWith("/")) relative = relative.substring(1);
        if (relative.endsWith("/")) relative = relative.substring(0, relative.length() - 1);
        return transmissionDownloadDir + "/" + relative;
    }

}

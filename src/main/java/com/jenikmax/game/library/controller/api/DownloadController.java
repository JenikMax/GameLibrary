package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.DownloadInfoResponse;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.downloads.DownloadTorrentService;
import com.jenikmax.game.library.service.downloads.aria2.Aria2Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final LibraryService libraryService;
    private final Aria2Service aria2Service;
    private final DownloadTorrentService torrentService;

    public DownloadController(LibraryService libraryService, Aria2Service aria2Service, DownloadTorrentService torrentService) {
        this.libraryService = libraryService;
        this.aria2Service = aria2Service;
        this.torrentService = torrentService;
    }

    @GetMapping("/games/{id}/download")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadGame(
            @PathVariable Long id, HttpServletResponse response) {
        logger.info("Download game - {}", id);
        GameDto gameDto = libraryService.getGameInfo(id);
        return libraryService.downloadGameInStream(gameDto, response);
    }

    @GetMapping("/games/{id}/download-info")
    public ResponseEntity<ApiResponse<DownloadInfoResponse>> getDownloadInfo(@PathVariable Long id) {
        GameDto gameDto = libraryService.getGameInfo(id);
        DownloadInfoResponse info = new DownloadInfoResponse();
        info.setGameId(gameDto.getId());
        info.setGameName(gameDto.getName());
        info.setReleaseDate(gameDto.getReleaseDate());
        info.setDownloadUrl("/game-library/api/games/" + id + "/download");
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @PostMapping("/games/{id}/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedGame(@PathVariable Long id) {
        logger.info("Seed game via aria2 - {}", id);
        try {
            GameDto gameDto = libraryService.getGameInfo(id);
            DownloadTorrentService.TorrentResult result =
                    torrentService.createTorrent(gameDto.getDirectoryPath(), true);

            Map<String, Object> data = new HashMap<>();
            data.put("gameId", id);
            data.put("gameName", gameDto.getName());
            data.put("torrentPath", result.getTorrentPath());
            data.put("aria2Gid", result.getAria2Gid());
            data.put("torrentDownloadUrl",
                    "/game-library/api/games/" + id + "/download");

            return ResponseEntity.ok(ApiResponse.ok("Torrent created and seeding started", data));
        } catch (Exception e) {
            logger.error("Seed game error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to seed game: " + e.getMessage()));
        }
    }

    @GetMapping("/downloads/active")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveDownloads() {
        List<Map<String, Object>> active = aria2Service.tellActive();
        List<Map<String, Object>> simplified = active.stream()
                .map(this::simplifyStatus)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(simplified));
    }

    @GetMapping("/downloads/waiting")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWaitingDownloads(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int num) {
        List<Map<String, Object>> waiting = aria2Service.tellWaiting(offset, num);
        return ResponseEntity.ok(ApiResponse.ok(waiting));
    }

    @GetMapping("/downloads/stopped")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoppedDownloads(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int num) {
        List<Map<String, Object>> stopped = aria2Service.tellStopped(offset, num);
        return ResponseEntity.ok(ApiResponse.ok(stopped));
    }

    @GetMapping("/downloads/{gid}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadStatus(@PathVariable String gid) {
        Map<String, Object> status = aria2Service.tellStatus(gid);
        if (status.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Download not found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(simplifyStatus(status)));
    }

    @PostMapping("/downloads/{gid}/remove")
    public ResponseEntity<ApiResponse<Void>> removeDownload(@PathVariable String gid) {
        boolean removed = aria2Service.remove(gid);
        if (removed) {
            return ResponseEntity.ok(ApiResponse.ok("Download removed", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to remove download"));
    }

    @PostMapping("/downloads/{gid}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseDownload(@PathVariable String gid) {
        boolean paused = aria2Service.pause(gid);
        if (paused) {
            return ResponseEntity.ok(ApiResponse.ok("Download paused", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to pause download"));
    }

    @PostMapping("/downloads/{gid}/unpause")
    public ResponseEntity<ApiResponse<Void>> unpauseDownload(@PathVariable String gid) {
        boolean unpaused = aria2Service.unpause(gid);
        if (unpaused) {
            return ResponseEntity.ok(ApiResponse.ok("Download resumed", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to resume download"));
    }

    @GetMapping("/downloads/global-stat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalStat() {
        Map<String, Object> stat = aria2Service.getGlobalStat();
        return ResponseEntity.ok(ApiResponse.ok(stat));
    }

    @GetMapping("/downloads/aria2-version")
    public ResponseEntity<ApiResponse<String>> getAria2Version() {
        boolean connected = aria2Service.isConnected();
        if (connected) {
            return ResponseEntity.ok(ApiResponse.ok("aria2 is connected"));
        }
        return ResponseEntity.status(503).body(ApiResponse.error("aria2 is not available"));
    }

    /**
     * Simplify aria2 status response for frontend consumption.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> simplifyStatus(Map<String, Object> raw) {
        Map<String, Object> simple = new HashMap<>();
        simple.put("gid", raw.get("gid"));
        simple.put("status", raw.get("status"));
        simple.put("totalLength", raw.get("totalLength"));
        simple.put("completedLength", raw.get("completedLength"));
        simple.put("downloadSpeed", raw.get("downloadSpeed"));
        simple.put("uploadSpeed", raw.get("uploadSpeed"));
        simple.put("dir", raw.get("dir"));

        if (raw.get("bittorrent") instanceof Map) {
            Map<String, Object> bt = (Map<String, Object>) raw.get("bittorrent");
            if (bt.get("info") instanceof Map) {
                Map<String, Object> info = (Map<String, Object>) bt.get("info");
                simple.put("name", info.get("name"));
            }
        }
        if (raw.get("files") instanceof List) {
            List<Map<String, Object>> files = (List<Map<String, Object>>) raw.get("files");
            simple.put("files", files);
            if (!files.isEmpty()) {
                simple.put("path", files.get(0).get("path"));
            }
        }

        // Calculate progress percentage
        long total = parseLong(raw.get("totalLength"));
        long completed = parseLong(raw.get("completedLength"));
        double progress = total > 0 ? (double) completed / total * 100.0 : 0.0;
        simple.put("progress", Math.round(progress * 100.0) / 100.0);

        return simple;
    }

    private long parseLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException e) { return 0; }
    }
}

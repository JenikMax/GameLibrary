package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.model.dto.GameDto;
import com.jenikmax.game.library.model.dto.api.ApiResponse;
import com.jenikmax.game.library.model.dto.api.DownloadInfoResponse;
import com.jenikmax.game.library.service.api.LibraryService;
import com.jenikmax.game.library.service.downloads.DownloadFileService;
import com.jenikmax.game.library.service.downloads.DownloadTorrentService;
import com.jenikmax.game.library.service.downloads.transmission.TransmissionService;
import com.jenikmax.game.library.service.torrent.TorrentTask;
import com.jenikmax.game.library.service.torrent.TorrentTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private static final Logger logger = LoggerFactory.getLogger(DownloadController.class);

    private final LibraryService libraryService;
    private final TransmissionService transmissionService;
    private final DownloadFileService downloadFileService;
    private final DownloadTorrentService torrentService;
    private final TorrentTaskService torrentTaskService;

    public DownloadController(LibraryService libraryService,
                               TransmissionService transmissionService,
                               DownloadFileService downloadFileService,
                               DownloadTorrentService torrentService,
                               TorrentTaskService torrentTaskService) {
        this.libraryService = libraryService;
        this.transmissionService = transmissionService;
        this.downloadFileService = downloadFileService;
        this.torrentService = torrentService;
        this.torrentTaskService = torrentTaskService;
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
        long gameSize = downloadFileService.getDirectorySizeRecursively(gameDto.getDirectoryPath());
        boolean torrentCached = torrentService.isTorrentCached(gameDto.getDirectoryPath());
        DownloadInfoResponse info = new DownloadInfoResponse();
        info.setGameId(gameDto.getId());
        info.setGameName(gameDto.getName());
        info.setReleaseDate(gameDto.getReleaseDate());
        info.setGameSize(gameSize);
        info.setTorrentCached(torrentCached);
        info.setDownloadUrl("/game-library/api/games/" + id + "/download");
        return ResponseEntity.ok(ApiResponse.ok(info));
    }

    @PostMapping("/games/{id}/seed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> seedGame(@PathVariable Long id) {
        logger.info("Seed game via Transmission - {}", id);
        try {
            GameDto gameDto = libraryService.getGameInfo(id);
            String taskId = torrentTaskService.submitSeedTask(id, gameDto.getDirectoryPath());

            Map<String, Object> data = new HashMap<>();
            data.put("gameId", id);
            data.put("taskId", taskId);
            data.put("statusUrl", "/game-library/api/seed/status/" + taskId);

            return ResponseEntity.accepted()
                    .body(ApiResponse.ok("Torrent creation started", data));
        } catch (Exception e) {
            logger.error("Seed game error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start seeding: " + e.getMessage()));
        }
    }

    @PostMapping("/games/{id}/prepare-download")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prepareDownload(@PathVariable Long id) {
        logger.info("Prepare torrent download for game - {}", id);
        try {
            GameDto gameDto = libraryService.getGameInfo(id);
            String taskId = torrentTaskService.submitDownloadTask(id, gameDto.getDirectoryPath());

            Map<String, Object> data = new HashMap<>();
            data.put("gameId", id);
            data.put("taskId", taskId);
            data.put("statusUrl", "/game-library/api/download/prepare-status/" + taskId);

            return ResponseEntity.accepted()
                    .body(ApiResponse.ok("Torrent preparation started", data));
        } catch (Exception e) {
            logger.error("Prepare download error", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to start torrent preparation: " + e.getMessage()));
        }
    }

    @GetMapping("/download/prepare-status/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrepareStatus(@PathVariable String taskId) {
        TorrentTask task = torrentTaskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        data.put("gameId", task.getGameId());
        data.put("status", task.getStatus().name());
        data.put("progress", task.getProgress());
        data.put("currentFile", task.getCurrentFile());
        data.put("errorMessage", task.getErrorMessage());

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/seed/status/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSeedStatus(@PathVariable String taskId) {
        TorrentTask task = torrentTaskService.getTask(taskId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getTaskId());
        data.put("gameId", task.getGameId());
        data.put("status", task.getStatus().name());
        data.put("progress", task.getProgress());
        data.put("currentFile", task.getCurrentFile());
        data.put("torrentPath", task.getTorrentPath());
        data.put("seedId", task.getSeedId());
        data.put("errorMessage", task.getErrorMessage());

        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/downloads/active")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActiveDownloads() {
        List<Map<String, Object>> active = transmissionService.getActive();
        return ResponseEntity.ok(ApiResponse.ok(active));
    }

    @GetMapping("/downloads/waiting")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWaitingDownloads(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int num) {
        List<Map<String, Object>> waiting = transmissionService.getWaiting();
        return ResponseEntity.ok(ApiResponse.ok(waiting));
    }

    @GetMapping("/downloads/stopped")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoppedDownloads(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int num) {
        List<Map<String, Object>> stopped = transmissionService.getStopped(offset, num);
        return ResponseEntity.ok(ApiResponse.ok(stopped));
    }

    @GetMapping("/downloads/{gid}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDownloadStatus(@PathVariable String gid) {
        Map<String, Object> status = transmissionService.getStatus(gid);
        if (status.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Download not found"));
        }
        return ResponseEntity.ok(ApiResponse.ok(status));
    }

    @PostMapping("/downloads/{gid}/remove")
    public ResponseEntity<ApiResponse<Void>> removeDownload(@PathVariable String gid) {
        boolean removed = transmissionService.remove(gid, false);
        if (removed) {
            return ResponseEntity.ok(ApiResponse.ok("Download removed", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to remove download"));
    }

    @PostMapping("/downloads/{gid}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseDownload(@PathVariable String gid) {
        boolean paused = transmissionService.stopTorrent(gid);
        if (paused) {
            return ResponseEntity.ok(ApiResponse.ok("Download paused", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to pause download"));
    }

    @PostMapping("/downloads/{gid}/unpause")
    public ResponseEntity<ApiResponse<Void>> unpauseDownload(@PathVariable String gid) {
        boolean unpaused = transmissionService.startTorrent(gid);
        if (unpaused) {
            return ResponseEntity.ok(ApiResponse.ok("Download resumed", null));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error("Failed to resume download"));
    }

    @GetMapping("/downloads/global-stat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGlobalStat() {
        Map<String, Object> stat = transmissionService.getGlobalStat();
        return ResponseEntity.ok(ApiResponse.ok(stat));
    }

    @GetMapping("/downloads/aria2-version")
    public ResponseEntity<ApiResponse<String>> getTransmissionStatus() {
        boolean connected = transmissionService.isConnected();
        if (connected) {
            return ResponseEntity.ok(ApiResponse.ok("Transmission is connected"));
        }
        return ResponseEntity.status(503).body(ApiResponse.error("Transmission is not available"));
    }

}

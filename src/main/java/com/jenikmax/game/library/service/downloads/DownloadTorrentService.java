package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.transmission.TransmissionService;
import com.jenikmax.game.library.service.torrent.TorrentCacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Component
public class DownloadTorrentService {

    private static final Logger logger = LoggerFactory.getLogger(DownloadTorrentService.class);

    private final TorrentCacheManager cacheManager;
    private final String announceUrl;
    private final TransmissionService transmissionService;

    public DownloadTorrentService(
            @Value("${game-library.games.torrent.directory-tmp:/torrentDirTmp}") String torrentDir,
            @Value("${game-library.tracker.announce-base-url:http://localhost:8080/game-library/api/tracker/announce}") String announceUrl,
            TransmissionService transmissionService) {
        this.cacheManager = new TorrentCacheManager(Paths.get(torrentDir));
        this.announceUrl = announceUrl;
        this.transmissionService = transmissionService;
    }

    public TorrentResult createTorrent(String directoryPath, boolean seedViaTransmission,
                                        GameTorrentCreator.ProgressCallback callback)
            throws IOException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);

        long totalSize = files.stream().mapToLong(File::length).sum();
        int pieceLength = GameTorrentCreator.selectPieceLength(totalSize);

        Path cachedTorrent = cacheManager.getCachedTorrent(directory, files).orElse(null);
        Path torrentFile;

        if (cachedTorrent != null) {
            torrentFile = cachedTorrent;
            logger.info("Cache HIT for {} ({} files)", directoryPath, files.size());
        } else {
            logger.info("Cache MISS for {} — creating torrent ({} files, {} MB, piece={}B)",
                    directoryPath, files.size(), totalSize / 1024 / 1024, pieceLength);

            List<List<URI>> announceList = buildAnnounceList();

            byte[] torrentData = GameTorrentCreator.createMultiFile(
                    directory, files, pieceLength, announceList, "GameLibrary", callback);

            torrentFile = cacheManager.saveTorrent(directory, torrentData, files);
        }

        String torrentPath = torrentFile.toString();
        String seedId = null;

        if (seedViaTransmission) {
            seedId = transmissionService.addTorrent(torrentPath, directoryPath);
        }

        return new TorrentResult(torrentPath, seedId);
    }

    public TorrentResult createTorrent(String directoryPath, boolean seedViaTransmission)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        return createTorrent(directoryPath, seedViaTransmission, null);
    }

    public String createTorrent(String directoryPath)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        return createTorrent(directoryPath, false, null).getTorrentPath();
    }

    public List<File> listGameFiles(String directoryPath) {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);
        return files;
    }

    public boolean isTorrentCached(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.isDirectory()) return false;
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);
        return cacheManager.getCachedTorrent(directory, files).isPresent();
    }

    public void serveTorrentFile(String directoryPath, OutputStream outputStream,
                                  CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture) {
        try {
            File directory = new File(directoryPath);
            List<File> files = new ArrayList<>();
            searchFiles(directory, files);
            Path cachedTorrent = cacheManager.getCachedTorrent(directory, files)
                    .orElseThrow(() -> new IOException("Torrent not cached for " + directoryPath));

            try (FileInputStream fis = new FileInputStream(cachedTorrent.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            completableFuture.complete(ResponseEntity.ok().build());
        } catch (Exception e) {
            logger.error("Failed to serve torrent for {}", directoryPath, e);
            completableFuture.completeExceptionally(e);
        }
    }

    private void searchFiles(File directory, List<File> files) {
        if (directory.isDirectory()) {
            File[] listed = directory.listFiles();
            if (listed != null) {
                for (File file : listed) {
                    if (file.isDirectory()) {
                        searchFiles(file, files);
                    } else {
                        files.add(file);
                    }
                }
            }
        }
    }

    private List<List<URI>> buildAnnounceList() {
        URI announceURI = URI.create(announceUrl);
        List<List<URI>> announceList = new ArrayList<>();
        List<URI> tier = new ArrayList<>();
        tier.add(announceURI);
        announceList.add(tier);
        return announceList;
    }

    public static class TorrentResult {
        private final String torrentPath;
        private final String seedId;

        public TorrentResult(String torrentPath, String seedId) {
            this.torrentPath = torrentPath;
            this.seedId = seedId;
        }

        public String getTorrentPath() { return torrentPath; }
        public String getSeedId() { return seedId; }
    }

}

package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.transmission.TransmissionService;
import com.turn.ttorrent.common.Torrent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DownloadTorrentService {

    private final String torrentDir;
    private final String announceUrl;
    private final TransmissionService transmissionService;

    public DownloadTorrentService(
            @Value("${game-library.games.torrent.directory-tmp:/torrentDirTmp}") String torrentDir,
            @Value("${game-library.tracker.announce-base-url:http://localhost:8080/game-library/api/tracker/announce}") String announceUrl,
            TransmissionService transmissionService) {
        this.torrentDir = torrentDir;
        this.announceUrl = announceUrl;
        this.transmissionService = transmissionService;
    }

    /**
     * Create a .torrent file with embedded tracker announce URL and optionally
     * start seeding via Transmission.
     *
     * @param directoryPath path to game directory
     * @param seedViaTransmission if true, sends the torrent to Transmission for seeding
     * @return result with torrent file path and optional seed ID
     */
    public TorrentResult createTorrent(String directoryPath, boolean seedViaTransmission)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);

        // Use embedded HTTP tracker as primary announce, DHT as fallback
        URI announceURI = URI.create(announceUrl);
        Torrent torrent = Torrent.create(directory, files, announceURI, "GameLibrary");

        String torrentFileName = torrent.getName() + new Date().getTime() + ".torrent";
        File result = new File(torrentDir + File.separator + torrentFileName);
        result.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(result)) {
            torrent.save(fos);
        }

        String torrentPath = result.getAbsolutePath();
        String seedId = null;

        if (seedViaTransmission) {
            seedId = transmissionService.addTorrent(torrentPath, directoryPath);
        }

        return new TorrentResult(torrentPath, seedId);
    }

    /**
     * Legacy method: create torrent without seeding.
     */
    public String createTorrent(String directoryPath)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        return createTorrent(directoryPath, false).getTorrentPath();
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

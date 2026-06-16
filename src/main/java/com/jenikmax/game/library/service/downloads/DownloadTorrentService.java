package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.aria2.Aria2Service;
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
    private final Aria2Service aria2Service;

    public DownloadTorrentService(
            @Value("${game-library.games.torrent.directory-tmp:/torrentDirTmp}") String torrentDir,
            Aria2Service aria2Service) {
        this.torrentDir = torrentDir;
        this.aria2Service = aria2Service;
    }

    /**
     * Create a .torrent file (DHT-only, no external tracker) and optionally
     * start seeding via aria2.
     *
     * @param directoryPath path to game directory
     * @param seedViaAria2  if true, sends the torrent to aria2 for seeding
     * @return result with torrent file path and optional aria2 GID
     */
    public TorrentResult createTorrent(String directoryPath, boolean seedViaAria2)
            throws IOException, InterruptedException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);

        // DHT-only: use a placeholder announce URI (some clients require one)
        // aria2 supports DHT + PEX without a tracker
        URI announceURI = URI.create("dht:://GameLibrary");
        Torrent torrent = Torrent.create(directory.getParentFile(), files, announceURI, "GameLibrary");

        String torrentFileName = torrent.getName() + new Date().getTime() + ".torrent";
        File result = new File(torrentDir + File.separator + torrentFileName);
        result.createNewFile();
        try (FileOutputStream fos = new FileOutputStream(result)) {
            torrent.save(fos);
        }

        String torrentPath = result.getAbsolutePath();
        String gid = null;

        if (seedViaAria2) {
            gid = aria2Service.addTorrent(torrentPath, directoryPath);
        }

        return new TorrentResult(torrentPath, gid);
    }

    /**
     * Legacy method: create torrent without aria2 seeding.
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
        private final String aria2Gid;

        public TorrentResult(String torrentPath, String aria2Gid) {
            this.torrentPath = torrentPath;
            this.aria2Gid = aria2Gid;
        }

        public String getTorrentPath() { return torrentPath; }
        public String getAria2Gid() { return aria2Gid; }
    }
}

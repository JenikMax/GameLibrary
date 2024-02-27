package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.api.TorrentTracker;
import com.turn.ttorrent.common.Torrent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class DownloadTorrentService {

    private final Long torrentTtl;
    private final String torrentDir;
    private final String torrentWatch;
    private final String trackerHost;
    private final int trackerPort;
    private final TorrentTracker tracker;

    public DownloadTorrentService(@Value("${game-library.games.torrent.ttl}") Long torrentTtl,
                                  @Value("${game-library.games.torrent.directory-tmp}") String torrentDir,
                                  @Value("${game-library.games.torrent.directory}") String torrentWatch,
                                  @Value("${game-library.games.torrent.tracker-host}") String trackerHost,
                                  @Value("${game-library.games.torrent.tracker-port}") int trackerPort, TorrentTracker tracker) {
        this.torrentTtl = torrentTtl;
        this.torrentDir = torrentDir;
        this.torrentWatch = torrentWatch;

        this.trackerHost = trackerHost;
        this.trackerPort = trackerPort;
        this.tracker = tracker;
    }

    public String createTorrent(String directoryPath) throws IOException, InterruptedException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);
        // Указываем IP-адрес хоста, где раздача будет доступна
        URI announceURI = URI.create("http://" + trackerHost + ":" + trackerPort + "/announce");
        Torrent torrent = Torrent.create(directory.getParentFile(), files, announceURI, "GameLibrary");
        tracker.annonceTorrent(torrent);
        //tracker.unAnnonceTorrent(torrent,torrentTtl);

        File result = new File(torrentDir + File.separator + torrent.getName() + new Date().getTime() + ".torrent");
        result.createNewFile();
        try(FileOutputStream fileOutputStream = new FileOutputStream(result);){
            torrent.save(fileOutputStream);
        }
        copyFile(result.getAbsolutePath(),torrentWatch);
        return result.getAbsolutePath();
    }

    private void copyFile(String sourceFilePath, String destinationDirectory) throws IOException {
        File sourceFile = new File(sourceFilePath);
        File destinationDir = new File(destinationDirectory);

        // Создание директории, если она не существует
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        // Путь назначения для скопированного файла
        String destinationFilePath = destinationDirectory + File.separator + sourceFile.getName();

        FileChannel sourceChannel = null;
        FileChannel destinationChannel = null;

        try {
            sourceChannel = new FileInputStream(sourceFile).getChannel();
            destinationChannel = new FileOutputStream(destinationFilePath).getChannel();
            // Копирование файла из исходного канала в канал назначения
            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

            System.out.println("Файл успешно скопирован в " + destinationFilePath);
        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (destinationChannel != null) {
                destinationChannel.close();
            }
        }
    }

    private void searchFiles(File directory, List<File> files) {
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.isDirectory()) {
                    searchFiles(file, files);
                } else {
                    files.add(file);
                }
            }
        }
    }

}

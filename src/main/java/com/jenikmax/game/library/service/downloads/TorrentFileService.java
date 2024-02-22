package com.jenikmax.game.library.service.downloads;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.jenikmax.game.library.service.downloads.api.TorrentTracker;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TorrentFileService {
    
    private final TorrentTracker tracker;
    private final Long torrentTtl;
    private final String torrentDir;

    public TorrentFileService(TorrentTracker tracker, @Value("${game-library.games.torrent.ttl}") Long torrentTtl,
                              @Value("${game-library.games.torrent.directory}") String torrentDir) {
        this.tracker = tracker;
        this.torrentTtl = torrentTtl;
        this.torrentDir = torrentDir;
    }

    public String createAndShare(String dirPath){
        try{
            String torrentFilePath = createTorrent(dirPath);
            initShareClient(torrentFilePath,dirPath);
            return torrentFilePath;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private String createTorrent(String directoryPath) throws IOException, InterruptedException, NoSuchAlgorithmException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);

        // Указываем IP-адрес хоста, где раздача будет доступна
        URI announceURI = URI.create("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + 9000 + "/announce");
        Torrent torrent = Torrent.create(directory, files, announceURI, "GameLibrary");
        tracker.annonceTorrent(torrent);
        tracker.unAnnonceTorrent(torrent,torrentTtl);


        File result = new File(torrentDir + File.separator + torrent.getName() + new Date().getTime() + ".torrent");
        result.createNewFile();

        FileOutputStream fileOutputStream = new FileOutputStream(result);
        torrent.save(fileOutputStream);
        fileOutputStream.close();

        return result.getAbsolutePath();
    }

    private void initShareClient(String torrentPath, String directoryPath) {
        try {
            File torrentFile = new File(torrentPath);
            File directoryFile = new File(directoryPath).getParentFile();
            Client client = new Client(InetAddress.getLocalHost(), SharedTorrent.fromFile(torrentFile, directoryFile));
            client.setMaxDownloadRate(0);
            client.setMaxUploadRate(0);
            client.share();

            // Создание ScheduledExecutorService с именем потока "ClientShutdownThread"
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder().setNameFormat("ClientShutdownThread").build()
            );

            // Флаг для отслеживания статуса раздачи
            AtomicBoolean isSeeding = new AtomicBoolean(true);

            // Запуск задачи по закрытию клиента через torrentTtl ms после начала раздачи
            scheduler.schedule(() -> {
                if (isSeeding.get()) {
                    client.stop();
                    isSeeding.set(false);
                    System.out.println("Seeding completed");
                }
            }, torrentTtl, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
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

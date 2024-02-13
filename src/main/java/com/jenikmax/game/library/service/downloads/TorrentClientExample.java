package com.jenikmax.game.library.service.downloads;

import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TorrentClientExample {
    public static Client client;
    public static Tracker tracker;
    public static String directoryPath = "D:/Work/MyProjects/gametest/games/pc";
    public static String subDirectoryPath = "/Cyberpunk 2077";
    public static String torrentDirectoryPath = "D:/Work/MyProjects/gametest/torrentDir";

    public static void main(String[] args) {
        try {
            startTracker();
            String torrentFilePath = createTorrent(directoryPath + subDirectoryPath);
            startSeeding(torrentFilePath);
            stopSeedingAfterDelay(3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startTracker() throws IOException {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),9000);
        tracker = new Tracker(address);
        System.out.println(tracker.getAnnounceUrl().toString());
        tracker.start();
        //try {
            //    // Создание трекера
            //    SimpleTracker tracker = new SimpleTracker(new InetSocketAddress("localhost", 6969));
            //
            //    // Запуск трекера
            //    tracker.start();
            //
            //    // Ожидание завершения работы трекера
            //    tracker.waitForExit();
            //} catch (IOException e) {
            //    e.printStackTrace();
            //}
    }

    private static String createTorrent(String directoryPath) throws IOException, InterruptedException {
        File directory = new File(directoryPath);
        List<File> files = new ArrayList<>();
        searchFiles(directory, files);

        // Указываем IP-адрес хоста, где раздача будет доступна
        URI announceURI = URI.create("http://" + InetAddress.getLocalHost().getHostAddress() + ":" + 9000 + "/announce");
        Torrent torrent = Torrent.create(directory, files, announceURI, "GameLibrary");
        tracker.announce(new TrackedTorrent(torrent));
        File result = new File(torrentDirectoryPath + File.separator + torrent.getName() + ".torrent");
        result.createNewFile();

        FileOutputStream fileOutputStream = new FileOutputStream(result);
        torrent.save(fileOutputStream);
        fileOutputStream.close();

        return result.getAbsolutePath();
    }

    private static void startSeeding(String torrentFilePath) throws IOException {
        SharedTorrent torrent = SharedTorrent.fromFile(new File(torrentFilePath), new File(directoryPath));
        client = new Client(InetAddress.getLocalHost(), torrent);
        client.share();
    }

    private static void stopSeedingAfterDelay(int hours) throws InterruptedException {
        TimeUnit.HOURS.sleep(hours);
        client.stop();
    }

    private static void searchFiles(File directory, List<File> files) {
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

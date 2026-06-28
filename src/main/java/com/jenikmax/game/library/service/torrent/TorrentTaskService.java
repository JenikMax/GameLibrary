package com.jenikmax.game.library.service.torrent;

import com.jenikmax.game.library.service.downloads.DownloadTorrentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TorrentTaskService implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(TorrentTaskService.class);

    private final Map<String, TorrentTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "torrent-creator");
        t.setDaemon(true);
        return t;
    });

    private final DownloadTorrentService downloadTorrentService;

    public TorrentTaskService(DownloadTorrentService downloadTorrentService) {
        this.downloadTorrentService = downloadTorrentService;
    }

    public String submitSeedTask(Long gameId, String directoryPath) {
        pruneOldTasks();
        String taskId = UUID.randomUUID().toString();
        TorrentTask task = new TorrentTask(taskId, gameId);
        tasks.put(taskId, task);

        executor.submit(() -> {
            try {
                task.setStatus(TorrentTask.Status.HASHING);

                DownloadTorrentService.TorrentResult result =
                        downloadTorrentService.createTorrent(directoryPath, true, (done, total, fileName) -> {
                            task.setProgress(done * 100 / total);
                            task.setCurrentFile(fileName);
                        });

                task.setTorrentPath(result.getTorrentPath());
                task.setSeedId(result.getSeedId());
                task.setStatus(TorrentTask.Status.COMPLETED);

            } catch (Exception e) {
                logger.error("Torrent creation failed for {}", directoryPath, e);
                task.setStatus(TorrentTask.Status.FAILED);
                task.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        });

        return taskId;
    }

    public String submitDownloadTask(Long gameId, String directoryPath) {
        pruneOldTasks();
        String taskId = UUID.randomUUID().toString();
        TorrentTask task = new TorrentTask(taskId, gameId);
        tasks.put(taskId, task);

        executor.submit(() -> {
            try {
                task.setStatus(TorrentTask.Status.HASHING);

                DownloadTorrentService.TorrentResult result =
                        downloadTorrentService.createTorrent(directoryPath, true, (done, total, fileName) -> {
                            task.setProgress(done * 100 / total);
                            task.setCurrentFile(fileName);
                        });

                task.setTorrentPath(result.getTorrentPath());
                task.setSeedId(result.getSeedId());
                task.setStatus(TorrentTask.Status.COMPLETED);

            } catch (Exception e) {
                logger.error("Torrent creation failed for {}", directoryPath, e);
                task.setStatus(TorrentTask.Status.FAILED);
                task.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown error");
            }
        });

        return taskId;
    }

    public TorrentTask getTask(String taskId) {
        return tasks.get(taskId);
    }

    @Scheduled(fixedRate = 60000)
    public void pruneOldTasks() {
        long now = System.currentTimeMillis();
        long keepMillis = 300000;
        Iterator<Map.Entry<String, TorrentTask>> it = tasks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, TorrentTask> entry = it.next();
            TorrentTask task = entry.getValue();
            if (task.getStatus() == TorrentTask.Status.COMPLETED
                    || task.getStatus() == TorrentTask.Status.FAILED) {
                if (now - task.getCreatedAt() > keepMillis) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }

}

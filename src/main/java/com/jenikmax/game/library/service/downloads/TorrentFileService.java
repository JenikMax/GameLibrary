package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.api.TorrentService;
import com.turn.ttorrent.common.Torrent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.URI;

@Service
public class TorrentFileService implements TorrentService {

    static final Logger logger = LogManager.getLogger(TorrentFileService.class.getName());

    @Override
    public void generateTorrent(String filesDir, String resultDir) {
        try {
            // Создание объекта SharedTorrent
            File directory = new File(filesDir);
            Torrent torrent = Torrent.create(directory, URI.create(InetAddress.getLocalHost().getHostAddress()), "");

            // Сохранение torrent-файла на диск
            FileOutputStream fileOutputStream = new FileOutputStream(resultDir);
            torrent.save(fileOutputStream);
            fileOutputStream.close();

            logger.info("Torrent-файл успешно создан.");

        } catch (Exception e) {
            logger.error("Error generateTorrent - ",e);
        }
    }
}

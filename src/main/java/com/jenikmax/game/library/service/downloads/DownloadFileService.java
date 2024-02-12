package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.api.DownloadService;

import com.turn.ttorrent.common.Torrent;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadFileService implements DownloadService {

    private final String torrentDir;

    public DownloadFileService(@Value("${game-library.games.torrent.directory}") String torrentDir) {
        this.torrentDir = torrentDir;
    }

    @Override
    public ByteArrayResource downloadZip(String path) {
        Path directoryPath = Paths.get(path);

        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return null;
        }

        // Create in-memory zip archive
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            Files.walk(directoryPath)
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String entryPath = directoryPath.relativize(file).toString();
                            ZipEntry zipEntry = new ZipEntry(entryPath);
                            zipOutputStream.putNextEntry(zipEntry);
                            try (InputStream inputStream = Files.newInputStream(file)) {
                                IOUtils.copy(inputStream, zipOutputStream);
                            }
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return new ByteArrayResource(outputStream.toByteArray());
    }


    @Override
    public ByteArrayResource downloadTorrent(String path) {
        return downloadTorrentCommon(path);
    }

    public ByteArrayResource downloadTorrentCommon(String path) {
        try{
            // Создание объекта SharedTorrent
            File directory = new File(path);
            List<File> files = new ArrayList<>();
            searchFiles(directory, files);
            Torrent torrent = Torrent.create(directory, files, URI.create(InetAddress.getLocalHost().getHostAddress()), "GameLibrary");
            // Сохранение torrent-файла на диск
            File result = new File(torrentDir + File.separator + torrent.getName() + ".torrent");
            result.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(result);
            torrent.save(fileOutputStream);
            fileOutputStream.close();

            byte[] fileData = new byte[(int) result.length()];
            try (InputStream inputStream = new FileInputStream(result)) {
                inputStream.read(fileData);
            }
            return new ByteArrayResource(fileData);

        }
        catch (Exception e){
            e.printStackTrace();
            return null;
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

    public long getDirectorySizeRecursively(String path) {
        File file = new File(path);
        long size = 0;
        if (file.isDirectory()) {
            for (File nestedFile : file.listFiles()) {
                size += getDirectorySizeRecursively(nestedFile.getPath());
            }
        } else {
            size += file.length();
        }
        return size;
    }

}

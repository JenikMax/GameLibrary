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
    private final String rpcUrl;
    private final String rpcUsername;
    private final String rpcPassword;

    public DownloadFileService(@Value("${game-library.games.torrent.directory}") String torrentDir,
                               @Value("${game-library.games.torrent.transmission.rpc-url}") String rpcUrl,
                               @Value("${game-library.games.torrent.transmission.rpc-username}") String rpcUsername,
                               @Value("${game-library.games.torrent.transmission.rpc-password}") String rpcPassword) {
        this.torrentDir = torrentDir;
        this.rpcUrl = rpcUrl;
        this.rpcUsername = rpcUsername;
        this.rpcPassword = rpcPassword;
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
        return downloadTorrentTransmission(path);
    }

    //@Override
    public ByteArrayResource downloadTorrent1(String path) {
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


    public ByteArrayResource downloadTorrentTransmission(String path) {
        try{
            File torrentFile = createTorrentFile(path, torrentDir);
            if (torrentFile != null && addTorrentToTransmissionDaemon(rpcUrl, rpcUsername, rpcPassword, torrentFile)) {
                byte[] fileData = new byte[(int) torrentFile.length()];
                try (InputStream inputStream = new FileInputStream(torrentFile)) {
                    inputStream.read(fileData);
                }
                return new ByteArrayResource(fileData);
            }
            return null;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private File createTorrentFile(String path, String torrentDir) throws IOException {
        File directory = new File(path);

        ProcessBuilder processBuilder = new ProcessBuilder(
                "transmission-create",
                "-o", torrentDir + File.separator + directory.getName() + ".torrent",
                "-t", "udp://tracker.example.com:1234",
                directory.getAbsolutePath()
        );

        Process process = processBuilder.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new File(torrentDir, directory.getName() + ".torrent");
    }

    private boolean addTorrentToTransmissionDaemon(String rpcUrl, String rpcUsername, String rpcPassword, File torrentFile) throws IOException {
        JSONObject requestBody = new JSONObject()
                .put("method", "torrent-add")
                .put("arguments", new JSONObject().put("filename", torrentFile.getAbsolutePath()));

        URL url = new URL(rpcUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Basic " +
                new String(Base64.getEncoder().encode((rpcUsername + ":" + rpcPassword).getBytes(StandardCharsets.UTF_8))));

        connection.setDoOutput(true);
        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            // Обработка ошибки при добавлении торрент-файла в Transmission
            System.out.println("Ошибка добавления торрент-файла");
            return false;
        }

        // Чтение ответа
        try (InputStream inputStream = connection.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            StreamUtils.copy(inputStream, outputStream);
            String responseBody = outputStream.toString(StandardCharsets.UTF_8.toString());

            JSONTokener tokener = new JSONTokener(responseBody);
            JSONObject responseJson = new JSONObject(tokener);

            if (responseJson.has("result") && "success".equals(responseJson.getString("result"))) {
                System.out.println("Торрент-файл успешно добавлен в Transmission");
                return true;
            } else {
                // Обработка ошибки при добавлении торрент-файла в Transmission
                System.out.println("Ошибка добавления торрент-файла: " + responseJson.optString("result"));
                return false;
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

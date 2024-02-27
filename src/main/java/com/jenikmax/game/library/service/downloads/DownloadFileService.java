package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.api.DownloadService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadFileService implements DownloadService {

    private static final int BUFFER_SIZE = 8192;

    private final DownloadTorrentService torrentService;

    public DownloadFileService(DownloadTorrentService torrentService) {
        this.torrentService = torrentService;
    }

    @Override
    public void downloadTorrent(String path, OutputStream outputStream, CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture) {
        try {
            Path directoryPath = Paths.get(path);
            String torrentFilePath = torrentService.createTorrent(path);
            File torrentFile = new File(torrentFilePath);
            if (!torrentFile.exists()) return;
            try(FileInputStream fis = new FileInputStream(torrentFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }catch (Exception e) {
                completableFuture.completeExceptionally(e);
            } finally {
                completableFuture.complete(ResponseEntity.ok().build());
            }
        }
        catch (Exception e){
            e.printStackTrace();
            completableFuture.completeExceptionally(e);
        }
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
            addFilesToZip(zipOutputStream, directoryPath, directoryPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ByteArrayResource(outputStream.toByteArray());
    }

    private void addFilesToZip(ZipOutputStream zipOutputStream, Path directoryPath, Path basePath) throws IOException {
        Files.walk(directoryPath)
                .filter(file -> !Files.isDirectory(file))
                .forEach(file -> {
                    try {
                        String entryPath = basePath.relativize(file).toString();
                        ZipEntry zipEntry = new ZipEntry(entryPath);
                        zipOutputStream.putNextEntry(zipEntry);

                        byte[] buffer = new byte[8192];
                        int bytesRead;

                        try (InputStream inputStream = Files.newInputStream(file)) {
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                zipOutputStream.write(buffer, 0, bytesRead);
                            }
                        }

                        zipOutputStream.closeEntry();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Override
    public void downloadZipInStream(String path, OutputStream outputStream, CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture) {
        Path directoryPath = Paths.get(path);

        if (!Files.exists(directoryPath) || !Files.isDirectory(directoryPath)) {
            return;
        }
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            addFilesToZip(zipOutputStream, directoryPath, directoryPath);
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        } finally {
            completableFuture.complete(ResponseEntity.ok().build());
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

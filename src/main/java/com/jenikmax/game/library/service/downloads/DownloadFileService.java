package com.jenikmax.game.library.service.downloads;

import com.jenikmax.game.library.service.downloads.api.DownloadService;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class DownloadFileService implements DownloadService {

    private final TorrentFileService torrentFileService;

    public DownloadFileService(TorrentFileService torrentFileService) {
        this.torrentFileService = torrentFileService;
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
        try {
            String torrentFilePath = torrentFileService.createAndShare(path);
            File result = new File(torrentFilePath);
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

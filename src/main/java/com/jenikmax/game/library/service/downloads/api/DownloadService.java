package com.jenikmax.game.library.service.downloads.api;

import com.jenikmax.game.library.service.downloads.StreamingZipWriter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public interface DownloadService {

    void downloadTorrent(String path, OutputStream outputStream, CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture);

    ByteArrayResource downloadZip(String path);

    void downloadZipInStream(String path, OutputStream outputStream, CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture);

    void serveCachedTorrent(String path, OutputStream outputStream,
                            CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture);

    long getDirectorySizeRecursively(String path);

    StreamingZipWriter.ZipManifest buildZipManifest(String path) throws IOException;

    void downloadZipWithManifest(String path, OutputStream outputStream, StreamingZipWriter.ZipManifest manifest,
                                  CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture);

    long getCachedTorrentSize(String path) throws IOException;

}

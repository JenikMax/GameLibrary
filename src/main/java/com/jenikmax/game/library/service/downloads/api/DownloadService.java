package com.jenikmax.game.library.service.downloads.api;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

public interface DownloadService {

    ByteArrayResource downloadZip(String path);

    void downloadZipInStream(String path, OutputStream outputStream, CompletableFuture<ResponseEntity<StreamingResponseBody>> completableFuture);


    long getDirectorySizeRecursively(String path);

}

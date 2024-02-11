package com.jenikmax.game.library.service.downloads.api;

import org.springframework.core.io.ByteArrayResource;

import java.io.ByteArrayOutputStream;

public interface DownloadService {

    ByteArrayResource downloadZip(String path);

    ByteArrayResource downloadTorrent(String path);

    long getDirectorySizeRecursively(String path);

}

package com.jenikmax.game.library.service.downloads.api;

import org.springframework.core.io.ByteArrayResource;

public interface DownloadService {

    ByteArrayResource downloadZip(String path);

    ByteArrayResource downloadTorrent(String path);

    long getDirectorySizeRecursively(String path);

}

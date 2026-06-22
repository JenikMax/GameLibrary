package com.jenikmax.game.library.service.torrent;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TorrentCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(TorrentCacheManager.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE);
    private static final String META_EXT = ".torrent.meta";

    private final Path cacheDir;

    public TorrentCacheManager(Path cacheDir) {
        this.cacheDir = cacheDir;
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            logger.warn("Cannot create torrent cache directory: {}", cacheDir);
        }
    }

    private Object lockFor(File gameDir) {
        return gameDir.getAbsolutePath().intern();
    }

    public Optional<Path> getCachedTorrent(File gameDir, List<File> currentFiles) {
        synchronized (lockFor(gameDir)) {
            String key = cacheKey(gameDir);
            Path metaPath = cacheDir.resolve(key + META_EXT);
            Path torrentPath = cacheDir.resolve(key + ".torrent");

            if (!Files.exists(metaPath) || !Files.exists(torrentPath)) {
                return Optional.empty();
            }

            CacheMeta saved = loadMeta(metaPath);
            if (saved == null) {
                return Optional.empty();
            }

            if (saved.files == null || saved.files.size() != currentFiles.size()) {
                return Optional.empty();
            }

            for (File f : currentFiles) {
                String relPath = relativize(gameDir, f);
                long size = f.length();
                long mtime = f.lastModified();

                CacheFileMeta cached = saved.files.get(relPath);
                if (cached == null || cached.size != size || cached.mtime != mtime) {
                    return Optional.empty();
                }
            }

            return Optional.of(torrentPath);
        }
    }

    public Path saveTorrent(File gameDir, byte[] torrentData, List<File> files) throws IOException {
        synchronized (lockFor(gameDir)) {
            String key = cacheKey(gameDir);
            Path torrentPath = cacheDir.resolve(key + ".torrent");
            Path metaPath = cacheDir.resolve(key + META_EXT);

            Files.write(torrentPath, torrentData);

            CacheMeta meta = new CacheMeta();
            meta.files = new HashMap<>();
            for (File f : files) {
                CacheFileMeta fm = new CacheFileMeta();
                fm.size = f.length();
                fm.mtime = f.lastModified();
                meta.files.put(relativize(gameDir, f), fm);
            }

            Files.write(metaPath, MAPPER.writeValueAsBytes(meta));

            logger.debug("Saved torrent cache: {} ({} files)", torrentPath, files.size());
            return torrentPath;
        }
    }

    public void invalidate(File gameDir) {
        synchronized (lockFor(gameDir)) {
            String key = cacheKey(gameDir);
            Path torrentPath = cacheDir.resolve(key + ".torrent");
            Path metaPath = cacheDir.resolve(key + META_EXT);
            try {
                Files.deleteIfExists(torrentPath);
                Files.deleteIfExists(metaPath);
            } catch (IOException e) {
                logger.warn("Failed to invalidate cache for {}", gameDir);
            }
        }
    }

    private CacheMeta loadMeta(Path metaPath) {
        try {
            byte[] data = Files.readAllBytes(metaPath);
            return MAPPER.readValue(data, CacheMeta.class);
        } catch (IOException e) {
            logger.warn("Failed to load torrent cache meta: {}", metaPath);
            return null;
        }
    }

    private static String cacheKey(File gameDir) {
        String absPath = gameDir.getAbsolutePath();
        int hash = absPath.hashCode();
        return gameDir.getName() + "_" + Integer.toHexString(hash);
    }

    static String relativize(File parent, File child) {
        return parent.toURI().relativize(child.toURI()).getPath();
    }

    static class CacheMeta {
        public Map<String, CacheFileMeta> files;
    }

    static class CacheFileMeta {
        public long size;
        public long mtime;
    }

}

package com.jenikmax.game.library.service.downloads;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class StreamingZipWriter {

    private static final int BUFFER_SIZE = 8192;

    public static class FileEntry {
        final String name;
        final long size;

        FileEntry(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }

    public static class ZipManifest {
        public final List<FileEntry> entries;
        public final long zipSize;

        ZipManifest(List<FileEntry> entries, long zipSize) {
            this.entries = entries;
            this.zipSize = zipSize;
        }
    }

    public ZipManifest buildManifest(Path baseDir) throws IOException {
        List<FileEntry> entries = new ArrayList<>();
        Files.walk(baseDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String name = baseDir.relativize(path).toString().replace('\\', '/');
                    try {
                        entries.add(new FileEntry(name, Files.size(path)));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        long zipSize = calculateZipSize(entries);
        return new ZipManifest(entries, zipSize);
    }

    static long calculateZipSize(List<FileEntry> entries) {
        long total = 0;
        for (FileEntry e : entries) {
            byte[] nameBytes = e.name.getBytes(StandardCharsets.UTF_8);
            total += 30L + nameBytes.length;
            total += e.size;
            total += 16;
        }
        for (FileEntry e : entries) {
            byte[] nameBytes = e.name.getBytes(StandardCharsets.UTF_8);
            total += 46L + nameBytes.length;
        }
        total += 22;
        return total;
    }

    public void writeZip(ZipManifest manifest, Path baseDir, OutputStream out) throws IOException {
        List<FileEntry> entries = manifest.entries;
        int n = entries.size();
        byte[] buffer = new byte[BUFFER_SIZE];
        long cdOffset = 0;

        long[][] cdMeta = new long[n][3];
        byte[][] cdNames = new byte[n][];

        for (int i = 0; i < n; i++) {
            FileEntry e = entries.get(i);
            byte[] nameBytes = e.name.getBytes(StandardCharsets.UTF_8);
            cdNames[i] = nameBytes;
            cdMeta[i][0] = cdOffset;
            cdMeta[i][1] = e.size;

            writeLfh(out, nameBytes);

            CRC32 crc = new CRC32();
            try (InputStream fis = Files.newInputStream(baseDir.resolve(e.name))) {
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    crc.update(buffer, 0, read);
                }
            }
            cdMeta[i][2] = crc.getValue();

            writeDd(out, (int) crc.getValue(), (int) e.size, (int) e.size);

            cdOffset += 30L + nameBytes.length + e.size + 16;
        }

        long cdOffsetFinal = cdOffset;
        int cdSize = 0;
        for (int i = 0; i < n; i++) {
            byte[] nameBytes = cdNames[i];
            writeCdfh(out, nameBytes,
                    (int) cdMeta[i][2],
                    (int) cdMeta[i][1],
                    (int) cdMeta[i][1],
                    (int) cdMeta[i][0]);
            cdSize += 46 + nameBytes.length;
        }

        writeEocd(out, n, cdSize, (int) cdOffsetFinal);
    }

    private static void writeLE(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
        out.write((value >> 16) & 0xFF);
        out.write((value >> 24) & 0xFF);
    }

    private static void writeLEShort(OutputStream out, int value) throws IOException {
        out.write(value & 0xFF);
        out.write((value >> 8) & 0xFF);
    }

    private static void writeLfh(OutputStream out, byte[] nameBytes) throws IOException {
        writeLE(out, 0x04034b50);
        writeLEShort(out, 20);
        writeLEShort(out, 0x0808);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLE(out, 0);
        writeLE(out, 0);
        writeLE(out, 0);
        writeLEShort(out, nameBytes.length);
        writeLEShort(out, 0);
        out.write(nameBytes);
    }

    private static void writeDd(OutputStream out, int crc32, int compressedSize, int uncompressedSize) throws IOException {
        writeLE(out, 0x08074b50);
        writeLE(out, crc32);
        writeLE(out, compressedSize);
        writeLE(out, uncompressedSize);
    }

    private static void writeCdfh(OutputStream out, byte[] nameBytes, int crc32, int compressedSize, int uncompressedSize, int localOffset) throws IOException {
        writeLE(out, 0x02014b50);
        writeLEShort(out, 20);
        writeLEShort(out, 20);
        writeLEShort(out, 0x0808);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLE(out, crc32);
        writeLE(out, compressedSize);
        writeLE(out, uncompressedSize);
        writeLEShort(out, nameBytes.length);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLE(out, 0);
        writeLE(out, localOffset);
        out.write(nameBytes);
    }

    private static void writeEocd(OutputStream out, int totalEntries, int cdSize, int cdOffset) throws IOException {
        writeLE(out, 0x06054b50);
        writeLEShort(out, 0);
        writeLEShort(out, 0);
        writeLEShort(out, totalEntries);
        writeLEShort(out, totalEntries);
        writeLE(out, cdSize);
        writeLE(out, cdOffset);
        writeLEShort(out, 0);
    }

}

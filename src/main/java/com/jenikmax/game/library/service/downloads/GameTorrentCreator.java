package com.jenikmax.game.library.service.downloads;

import com.turn.ttorrent.bcodec.BEValue;
import com.turn.ttorrent.bcodec.BEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GameTorrentCreator {

    public static final int PIECE_HASH_SIZE = 20;

    @FunctionalInterface
    public interface ProgressCallback {
        void onProgress(int piecesHashed, int totalPieces, String currentFileName);
    }

    public static byte[] createMultiFile(File parentDir, List<File> files, int pieceLength,
                                          List<List<URI>> announceList, String createdBy,
                                          ProgressCallback callback) throws IOException, NoSuchAlgorithmException {
        return create(parentDir, files, pieceLength, announceList, createdBy, callback);
    }

    public static byte[] createSingleFile(File file, int pieceLength,
                                           List<List<URI>> announceList, String createdBy,
                                           ProgressCallback callback) throws IOException, NoSuchAlgorithmException {
        List<File> single = new ArrayList<>();
        single.add(file);
        return create(file, single, pieceLength, announceList, createdBy, callback);
    }

    private static byte[] create(File source, List<File> files, int pieceLength,
                                  List<List<URI>> announceList, String createdBy,
                                  ProgressCallback callback) throws IOException, NoSuchAlgorithmException {

        Map<String, BEValue> root = new HashMap<>();

        if (announceList != null && !announceList.isEmpty()
                && !announceList.get(0).isEmpty()) {
            URI primary = announceList.get(0).get(0);
            root.put("announce", new BEValue(primary.toString()));
        }

        if (announceList != null) {
            List<BEValue> tiers = new LinkedList<>();
            for (List<URI> trackers : announceList) {
                List<BEValue> tierInfo = new LinkedList<>();
                for (URI trackerURI : trackers) {
                    tierInfo.add(new BEValue(trackerURI.toString()));
                }
                tiers.add(new BEValue(tierInfo));
            }
            root.put("announce-list", new BEValue(tiers));
        }

        root.put("creation date", new BEValue(System.currentTimeMillis() / 1000));
        root.put("created by", new BEValue(createdBy != null ? createdBy : "GameLibrary"));

        Map<String, BEValue> info = new TreeMap<>();
        info.put("name", new BEValue(source.getName()));
        info.put("piece length", new BEValue(pieceLength));

        if (files == null || files.isEmpty()) {
            info.put("length", new BEValue(source.length()));
            byte[] pieces = hashPieces(source.getName(), singleList(source), pieceLength, callback);
            info.put("pieces", new BEValue(pieces));
        } else {
            List<BEValue> fileInfo = buildFileInfoList(files, source);
            info.put("files", new BEValue(fileInfo));
            byte[] pieces = hashPieces(source.getName(), files, pieceLength, callback);
            info.put("pieces", new BEValue(pieces));
        }

        root.put("info", new BEValue(info));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BEncoder.bencode(new BEValue(root), baos);
        return baos.toByteArray();
    }

    private static List<File> singleList(File file) {
        List<File> list = new ArrayList<>();
        list.add(file);
        return list;
    }

    static List<BEValue> buildFileInfoList(List<File> files, File parentDir) throws IOException {
        List<BEValue> result = new LinkedList<>();
        for (File file : files) {
            Map<String, BEValue> fileMap = new HashMap<>();
            fileMap.put("length", new BEValue(file.length()));

            LinkedList<BEValue> pathComponents = new LinkedList<>();
            File current = file;
            while (current != null && !current.equals(parentDir)) {
                pathComponents.addFirst(new BEValue(current.getName()));
                current = current.getParentFile();
            }
            fileMap.put("path", new BEValue(pathComponents));
            result.add(new BEValue(fileMap));
        }
        return result;
    }

    public static byte[] hashPieces(String gameName, List<File> files, int pieceLength,
                                     ProgressCallback callback) throws IOException, NoSuchAlgorithmException {
        byte[] buffer = new byte[pieceLength];
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        ByteArrayOutputStream piecesOut = new ByteArrayOutputStream();

        long totalSize = 0;
        for (File f : files) {
            totalSize += f.length();
        }
        if (totalSize == 0) {
            return new byte[0];
        }
        int totalPieces = (int) ((totalSize + pieceLength - 1) / pieceLength);
        int hashedPieces = 0;

        ByteBuffer bb = ByteBuffer.wrap(buffer);
        bb.clear();

        for (File file : files) {
            String fileName = file.getName();
            try (FileChannel ch = FileChannel.open(file.toPath(), StandardOpenOption.READ)) {
                while (bb.position() < pieceLength) {
                    bb.limit(pieceLength);
                    int bytesRead = ch.read(bb);
                    if (bytesRead == -1) {
                        break;
                    }
                    if (bb.position() == pieceLength) {
                        md.update(buffer, 0, pieceLength);
                        byte[] hash = md.digest();
                        piecesOut.write(hash);
                        md.reset();
                        hashedPieces++;
                        if (callback != null) {
                            callback.onProgress(hashedPieces, totalPieces, fileName);
                        }
                        bb.clear();
                    }
                }
            }
        }

        if (bb.position() > 0) {
            md.update(buffer, 0, bb.position());
            byte[] hash = md.digest();
            piecesOut.write(hash);
            hashedPieces++;
            if (callback != null) {
                callback.onProgress(hashedPieces, totalPieces, files.get(files.size() - 1).getName());
            }
        }

        return piecesOut.toByteArray();
    }

    public static int selectPieceLength(long totalSize) {
        if (totalSize > 200L * 1024 * 1024 * 1024) return 16 * 1024 * 1024;
        if (totalSize > 50L  * 1024 * 1024 * 1024) return 8 * 1024 * 1024;
        if (totalSize > 10L  * 1024 * 1024 * 1024) return 4 * 1024 * 1024;
        if (totalSize > 3L   * 1024 * 1024 * 1024) return 2 * 1024 * 1024;
        return 1 * 1024 * 1024;
    }

}

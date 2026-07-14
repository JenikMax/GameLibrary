package com.jenikmax.game.library.service.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TrackerService {

    private static final Logger logger = LoggerFactory.getLogger(TrackerService.class);
    private static final long PEER_TIMEOUT_SEC = 1800;
    private static final long ANNOUNCE_INTERVAL_SEC = 1800;

    private final Map<String, List<PeerInfo>> peersByInfoHash = new ConcurrentHashMap<>();

    private int port;

    @Value("${server.port:8080}")
    public void setPort(int port) {
        this.port = port;
    }

    @PostConstruct
    public void init() {
        logger.info("TrackerService initialized on port {}", port);
    }

    public synchronized TrackerResponse announce(
            String infoHash, String peerId, String ip, int port,
            long uploaded, long downloaded, long left, String event) {

        List<PeerInfo> peers = peersByInfoHash.computeIfAbsent(infoHash,
                k -> new CopyOnWriteArrayList<>());

        if ("stopped".equals(event)) {
            peers.removeIf(p -> p.getPeerId().equals(peerId));
            logger.info("Peer {} left swarm for info_hash {}", peerId, infoHash);
        } else {
            PeerInfo existing = null;
            for (PeerInfo p : peers) {
                if (p.getPeerId().equals(peerId)) {
                    existing = p;
                    break;
                }
            }
            if (existing != null) {
                existing.update(uploaded, downloaded, left);
            } else {
                peers.add(new PeerInfo(peerId, ip, port, uploaded, downloaded, left));
                logger.info("Peer {} joined swarm for info_hash {}", peerId, infoHash);
            }
        }

        List<PeerInfo> activePeers = new ArrayList<>();
        int complete = 0;
        int incomplete = 0;
        for (PeerInfo p : peers) {
            if (!p.getPeerId().equals(peerId) && !p.isExpired(PEER_TIMEOUT_SEC)) {
                activePeers.add(p);
                if (p.isSeeder()) complete++;
                else incomplete++;
            }
        }

        logger.info("Swarm {}: {} peer(s), returning complete={}, incomplete={} to {}",
                infoHash, peers.size(), complete, incomplete, peerId);

        return new TrackerResponse(complete, incomplete, ANNOUNCE_INTERVAL_SEC, activePeers);
    }

    @Scheduled(fixedRate = 300000)
    public void cleanup() {
        for (Map.Entry<String, List<PeerInfo>> entry : peersByInfoHash.entrySet()) {
            List<PeerInfo> peers = entry.getValue();
            peers.removeIf(p -> p.isExpired(PEER_TIMEOUT_SEC));
            if (peers.isEmpty()) {
                peersByInfoHash.remove(entry.getKey());
                logger.debug("Removed empty swarm for info_hash {}", entry.getKey());
            }
        }
    }

    public String bencodeResponse(TrackerResponse response) {
        StringBuilder sb = new StringBuilder();
        sb.append("d");
        appendBencoded(sb, "complete");
        sb.append("i").append(response.complete).append("e");
        appendBencoded(sb, "incomplete");
        sb.append("i").append(response.incomplete).append("e");
        appendBencoded(sb, "interval");
        sb.append("i").append(response.interval).append("e");
        appendBencoded(sb, "peers");
        sb.append("l");
        for (PeerInfo p : response.peers) {
            sb.append("d");
            appendBencoded(sb, "ip");
            appendBencoded(sb, p.getIp());
            appendBencoded(sb, "peer id");
            appendBencoded(sb, p.getPeerId());
            appendBencoded(sb, "port");
            sb.append("i").append(p.getPort()).append("e");
            sb.append("e");
        }
        sb.append("e");
        sb.append("e");
        return sb.toString();
    }

    private void appendBencoded(StringBuilder sb, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.ISO_8859_1);
        sb.append(bytes.length).append(":");
        sb.append(value);
    }

    public String bencodeScrape(String infoHash) {
        StringBuilder sb = new StringBuilder();
        sb.append("d5:filesd");
        if (infoHash != null) {
            List<PeerInfo> peers = peersByInfoHash.get(infoHash);
            int complete = 0;
            int incomplete = 0;
            if (peers != null) {
                for (PeerInfo p : peers) {
                    if (p.isExpired(PEER_TIMEOUT_SEC)) continue;
                    if (p.isSeeder()) complete++;
                    else incomplete++;
                }
            }
            appendBencoded(sb, infoHash);
            sb.append("d");
            appendBencoded(sb, "complete");
            sb.append("i").append(complete).append("e");
            appendBencoded(sb, "incomplete");
            sb.append("i").append(incomplete).append("e");
            appendBencoded(sb, "downloaded");
            sb.append("i0e");
            sb.append("e");
        }
        sb.append("ee");
        return sb.toString();
    }

    public static class TrackerResponse {
        public final int complete;
        public final int incomplete;
        public final long interval;
        public final List<PeerInfo> peers;

        public TrackerResponse(int complete, int incomplete, long interval, List<PeerInfo> peers) {
            this.complete = complete;
            this.incomplete = incomplete;
            this.interval = interval;
            this.peers = peers;
        }
    }

}

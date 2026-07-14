package com.jenikmax.game.library.controller.api;

import com.jenikmax.game.library.service.tracker.TrackerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/tracker")
public class TrackerController {

    private static final Logger logger = LoggerFactory.getLogger(TrackerController.class);

    private final TrackerService trackerService;

    public TrackerController(TrackerService trackerService) {
        this.trackerService = trackerService;
    }

    @GetMapping(value = "/announce", produces = "text/plain; charset=ISO-8859-1")
    public ResponseEntity<String> announce(
            @RequestParam("info_hash") String infoHash,
            @RequestParam("peer_id") String peerId,
            @RequestParam("port") int port,
            @RequestParam(value = "uploaded", defaultValue = "0") long uploaded,
            @RequestParam(value = "downloaded", defaultValue = "0") long downloaded,
            @RequestParam(value = "left", defaultValue = "0") long left,
            @RequestParam(value = "event", defaultValue = "") String event,
            @RequestParam(value = "ip", required = false) String ipParam,
            @RequestParam(value = "compact", defaultValue = "0") int compact,
            HttpServletRequest request) {

        String ip = ipParam;
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
            // Docker internal IP → use the actual IP the peer connected to (from Host header)
            if (ip.startsWith("172.") || ip.startsWith("10.") || ip.startsWith("100.")) {
                String host = request.getHeader("Host");
                if (host != null) {
                    int colon = host.indexOf(':');
                    if (colon > 0) host = host.substring(0, colon);
                    if (!host.isEmpty()) ip = host;
                }
            }
        }

        logger.info("Tracker announce: info_hash_hex={}, peer_id={}, port={}, event={}, ip={}",
                toHex(infoHash), peerId, port, event, ip);

        TrackerService.TrackerResponse response = trackerService.announce(
                infoHash, peerId, ip, port, uploaded, downloaded, left, event);

        String bencoded = trackerService.bencodeResponse(response);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=ISO-8859-1"))
                .body(bencoded);
    }

    @GetMapping(value = "/scrape", produces = "text/plain; charset=ISO-8859-1")
    public ResponseEntity<String> scrape(
            @RequestParam(value = "info_hash", required = false) String infoHash) {
        logger.info("Tracker scrape: info_hash_hex={}", infoHash != null ? toHex(infoHash) : "null");
        String bencoded = trackerService.bencodeScrape(infoHash);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=ISO-8859-1"))
                .body(bencoded);
    }

    private static String toHex(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

}

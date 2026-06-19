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

import javax.servlet.http.HttpServletRequest;

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

        String ip = ipParam != null && !ipParam.isEmpty() ? ipParam : request.getRemoteAddr();

        if (logger.isDebugEnabled()) {
            logger.debug("Tracker announce: info_hash={}, peer_id={}, port={}, event={}, ip={}",
                    infoHash, peerId, port, event, ip);
        }

        TrackerService.TrackerResponse response = trackerService.announce(
                infoHash, peerId, ip, port, uploaded, downloaded, left, event);

        String bencoded = trackerService.bencodeResponse(response);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=ISO-8859-1"))
                .body(bencoded);
    }

}

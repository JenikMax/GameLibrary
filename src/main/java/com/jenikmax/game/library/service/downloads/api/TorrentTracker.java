package com.jenikmax.game.library.service.downloads.api;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;

@Component
public class TorrentTracker {

    private Tracker tracker;

    @Value("${game-library.games.torrent.tracker-port}")
    private int port;

    private static TorrentTracker instance;

    private TorrentTracker() {
        // пустой конструктор
    }

    @PostConstruct
    private void init() {
        try {
            tracker = new Tracker(new InetSocketAddress(port));
            tracker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized TorrentTracker getInstance() {
        if (instance == null) {
            instance = new TorrentTracker();
        }
        return instance;
    }

    public void annonceTorrent(Torrent torrent) {
        try {
            tracker.announce(new TrackedTorrent(torrent));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unAnnonceTorrent(Torrent torrent,long ms) {
        tracker.remove(torrent,ms);
    }
}

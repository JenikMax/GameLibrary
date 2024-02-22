package com.jenikmax.game.library.service.downloads.api;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TorrentTracker {

    private final Tracker tracker;

    private TorrentTracker(Tracker tracker) {
        this.tracker = tracker;
        this.tracker.start();
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

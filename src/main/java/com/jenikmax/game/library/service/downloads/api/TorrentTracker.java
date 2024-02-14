package com.jenikmax.game.library.service.downloads.api;

import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

import java.net.InetSocketAddress;

public class TorrentTracker {

    private Tracker tracker;

    private static TorrentTracker instance;

    private TorrentTracker(){
        // init
        try{
            tracker = new Tracker(new InetSocketAddress(9000));
            tracker.start();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public static synchronized TorrentTracker getInstance() {
        if (instance == null) {
            instance = new TorrentTracker();
        }
        return instance;
    }

    public void annonceTorrent(Torrent torrent){
        try{
            tracker.announce(new TrackedTorrent(torrent));
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void unAnnonceTorrent(Torrent torrent,long ms){
        tracker.remove(torrent,ms);
    }




}

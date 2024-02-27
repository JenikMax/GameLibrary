package com.jenikmax.game.library.config;

import com.turn.ttorrent.tracker.Tracker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;

@Configuration
public class TrackerConfig {

    @Value("${game-library.games.torrent.tracker-port}")
    private int trackerPort;

    @Value("${game-library.games.torrent.tracker-host}")
    private String trackerHost;

    @Bean
    public Tracker getTracker() throws IOException {
        Tracker tracker = new Tracker(new InetSocketAddress(trackerPort));
        // DO SOMETHING
        return tracker;
    }

}

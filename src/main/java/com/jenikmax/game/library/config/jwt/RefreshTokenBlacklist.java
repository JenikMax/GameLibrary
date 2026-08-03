package com.jenikmax.game.library.config.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class RefreshTokenBlacklist {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenBlacklist.class);

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "token-blacklist-cleanup");
        t.setDaemon(true);
        return t;
    });

    public RefreshTokenBlacklist() {
        cleanup.scheduleAtFixedRate(this::removeExpired, 1, 1, TimeUnit.HOURS);
    }

    public void blacklist(String jti, Instant expiry) {
        blacklist.put(jti, expiry);
        logger.debug("Refresh token blacklisted: {}", jti);
    }

    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);
    }

    private void removeExpired() {
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}

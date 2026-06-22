package com.jenikmax.game.library.service.tracker;

public class PeerInfo {

    private final String peerId;
    private final String ip;
    private final int port;
    private long uploaded;
    private long downloaded;
    private long left;
    private long lastSeen;

    public PeerInfo(String peerId, String ip, int port, long uploaded, long downloaded, long left) {
        this.peerId = peerId;
        this.ip = ip;
        this.port = port;
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
        this.lastSeen = System.currentTimeMillis() / 1000;
    }

    public String getPeerId() { return peerId; }
    public String getIp() { return ip; }
    public int getPort() { return port; }
    public long getUploaded() { return uploaded; }
    public long getDownloaded() { return downloaded; }
    public long getLeft() { return left; }
    public long getLastSeen() { return lastSeen; }

    public void update(long uploaded, long downloaded, long left) {
        this.uploaded = uploaded;
        this.downloaded = downloaded;
        this.left = left;
        this.lastSeen = System.currentTimeMillis() / 1000;
    }

    public boolean isExpired(long maxAgeSec) {
        return (System.currentTimeMillis() / 1000 - lastSeen) > maxAgeSec;
    }

    public boolean isSeeder() {
        return left == 0;
    }

}

package com.jenikmax.game.library.service.torrent;

public class TorrentTask {

    public enum Status {
        PENDING,
        HASHING,
        COMPLETED,
        FAILED
    }

    private final String taskId;
    private final Long gameId;
    private volatile Status status;
    private volatile int progress;
    private volatile String currentFile;
    private volatile String torrentPath;
    private volatile String seedId;
    private volatile String errorMessage;
    private final long createdAt;

    public TorrentTask(String taskId, Long gameId) {
        this.taskId = taskId;
        this.gameId = gameId;
        this.status = Status.PENDING;
        this.progress = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getTaskId() {
        return taskId;
    }

    public Long getGameId() {
        return gameId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getCurrentFile() {
        return currentFile;
    }

    public void setCurrentFile(String currentFile) {
        this.currentFile = currentFile;
    }

    public String getTorrentPath() {
        return torrentPath;
    }

    public void setTorrentPath(String torrentPath) {
        this.torrentPath = torrentPath;
    }

    public String getSeedId() {
        return seedId;
    }

    public void setSeedId(String seedId) {
        this.seedId = seedId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

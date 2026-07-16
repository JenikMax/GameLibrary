package com.jenikmax.game.library.service.scaner;

public class ScanTask {

    public enum Status {
        PENDING,
        SCANNING_DIRS,
        STORING_METADATA,
        LOADING_IMAGES,
        COMPLETED,
        FAILED
    }

    private final String taskId;
    private volatile Status status;
    private volatile int progress;
    private volatile String currentGame;
    private volatile int newGamesCount;
    private volatile int deletedGamesCount;
    private volatile int totalCount;
    private volatile String errorMessage;
    private volatile boolean notified;
    private final long createdAt;

    public ScanTask(String taskId) {
        this.taskId = taskId;
        this.status = Status.PENDING;
        this.progress = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getTaskId() {
        return taskId;
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

    public String getCurrentGame() {
        return currentGame;
    }

    public void setCurrentGame(String currentGame) {
        this.currentGame = currentGame;
    }

    public int getNewGamesCount() {
        return newGamesCount;
    }

    public void setNewGamesCount(int newGamesCount) {
        this.newGamesCount = newGamesCount;
    }

    public int getDeletedGamesCount() {
        return deletedGamesCount;
    }

    public void setDeletedGamesCount(int deletedGamesCount) {
        this.deletedGamesCount = deletedGamesCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

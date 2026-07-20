package com.jenikmax.game.library.service.ai;

public class EmbeddingTask {

    public enum Status {
        PENDING, GENERATING, COMPLETED, FAILED
    }

    private final String taskId;
    private volatile Status status = Status.PENDING;
    private volatile int progress;
    private volatile int processedCount;
    private volatile int totalCount;
    private volatile String currentGame;
    private volatile String errorMessage;
    private volatile boolean notified;
    private final long createdAt = System.currentTimeMillis();

    public EmbeddingTask(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() { return taskId; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getProcessedCount() { return processedCount; }
    public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
    public String getCurrentGame() { return currentGame; }
    public void setCurrentGame(String currentGame) { this.currentGame = currentGame; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public boolean isNotified() { return notified; }
    public void setNotified(boolean notified) { this.notified = notified; }
    public long getCreatedAt() { return createdAt; }
}

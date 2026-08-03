package com.jenikmax.game.library.model.dto.api;

import java.util.List;
import java.util.Map;

public class LibraryHealthReport {

    private int totalGames;
    private double healthScore;
    private Map<String, IssueCount> issueCounts;
    private List<GameIssue> topIssues;

    public int getTotalGames() { return totalGames; }
    public void setTotalGames(int totalGames) { this.totalGames = totalGames; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    public Map<String, IssueCount> getIssueCounts() { return issueCounts; }
    public void setIssueCounts(Map<String, IssueCount> issueCounts) { this.issueCounts = issueCounts; }

    public List<GameIssue> getTopIssues() { return topIssues; }
    public void setTopIssues(List<GameIssue> topIssues) { this.topIssues = topIssues; }

    public static class IssueCount {
        private String code;
        private String label;
        private int count;
        private boolean fixable;

        public IssueCount() {}
        public IssueCount(String code, String label, int count, boolean fixable) {
            this.code = code; this.label = label; this.count = count; this.fixable = fixable;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public boolean isFixable() { return fixable; }
        public void setFixable(boolean fixable) { this.fixable = fixable; }
    }

    public static class GameIssue {
        private long gameId;
        private String gameName;
        private String platform;
        private String issueType;
        private String issueDetail;
        private boolean fixable;

        public GameIssue() {}
        public GameIssue(long gameId, String gameName, String platform, String issueType, String issueDetail, boolean fixable) {
            this.gameId = gameId; this.gameName = gameName; this.platform = platform;
            this.issueType = issueType; this.issueDetail = issueDetail; this.fixable = fixable;
        }

        public long getGameId() { return gameId; }
        public void setGameId(long gameId) { this.gameId = gameId; }
        public String getGameName() { return gameName; }
        public void setGameName(String gameName) { this.gameName = gameName; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getIssueType() { return issueType; }
        public void setIssueType(String issueType) { this.issueType = issueType; }
        public String getIssueDetail() { return issueDetail; }
        public void setIssueDetail(String issueDetail) { this.issueDetail = issueDetail; }
        public boolean isFixable() { return fixable; }
        public void setFixable(boolean fixable) { this.fixable = fixable; }
    }
}

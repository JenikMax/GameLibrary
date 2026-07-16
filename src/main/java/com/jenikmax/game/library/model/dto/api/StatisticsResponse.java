package com.jenikmax.game.library.model.dto.api;

import java.util.List;
import java.util.Map;

public class StatisticsResponse {

    private long totalGames;
    private long totalSizeBytes;
    private long recentAdditions;
    private double averageRating;
    private List<CountItem> gamesByPlatform;
    private List<GenreCountItem> gamesByGenre;
    private List<CountItem> gamesByYear;
    private List<GameRatingItem> topRated;
    private List<GameFavItem> mostFavorited;
    private List<GameRatingItem> mostRated;

    public long getTotalGames() { return totalGames; }
    public void setTotalGames(long totalGames) { this.totalGames = totalGames; }

    public long getTotalSizeBytes() { return totalSizeBytes; }
    public void setTotalSizeBytes(long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }

    public long getRecentAdditions() { return recentAdditions; }
    public void setRecentAdditions(long recentAdditions) { this.recentAdditions = recentAdditions; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public List<CountItem> getGamesByPlatform() { return gamesByPlatform; }
    public void setGamesByPlatform(List<CountItem> gamesByPlatform) { this.gamesByPlatform = gamesByPlatform; }

    public List<GenreCountItem> getGamesByGenre() { return gamesByGenre; }
    public void setGamesByGenre(List<GenreCountItem> gamesByGenre) { this.gamesByGenre = gamesByGenre; }

    public List<CountItem> getGamesByYear() { return gamesByYear; }
    public void setGamesByYear(List<CountItem> gamesByYear) { this.gamesByYear = gamesByYear; }

    public List<GameRatingItem> getTopRated() { return topRated; }
    public void setTopRated(List<GameRatingItem> topRated) { this.topRated = topRated; }

    public List<GameFavItem> getMostFavorited() { return mostFavorited; }
    public void setMostFavorited(List<GameFavItem> mostFavorited) { this.mostFavorited = mostFavorited; }

    public List<GameRatingItem> getMostRated() { return mostRated; }
    public void setMostRated(List<GameRatingItem> mostRated) { this.mostRated = mostRated; }

    public static class CountItem {
        private String label;
        private long count;

        public CountItem() {}
        public CountItem(String label, long count) { this.label = label; this.count = count; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class GenreCountItem {
        private String code;
        private String name;
        private long count;

        public GenreCountItem() {}
        public GenreCountItem(String code, String name, long count) { this.code = code; this.name = name; this.count = count; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
    }

    public static class GameRatingItem {
        private long id;
        private String name;
        private double avgRating;
        private long ratingCount;

        public GameRatingItem() {}
        public GameRatingItem(long id, String name, double avgRating, long ratingCount) {
            this.id = id; this.name = name; this.avgRating = avgRating; this.ratingCount = ratingCount;
        }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getAvgRating() { return avgRating; }
        public void setAvgRating(double avgRating) { this.avgRating = avgRating; }
        public long getRatingCount() { return ratingCount; }
        public void setRatingCount(long ratingCount) { this.ratingCount = ratingCount; }
    }

    public static class GameFavItem {
        private long id;
        private String name;
        private long favoriteCount;

        public GameFavItem() {}
        public GameFavItem(long id, String name, long favoriteCount) { this.id = id; this.name = name; this.favoriteCount = favoriteCount; }

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(long favoriteCount) { this.favoriteCount = favoriteCount; }
    }
}

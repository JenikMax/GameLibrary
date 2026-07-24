package com.jenikmax.game.library.model.dto.api;

public class UserProfileResponse {

    private Long id;
    private String name;
    private boolean admin;
    private boolean active;
    private String avatarUrl;
    private String memberSince;
    private long gamesCount;
    private long ratingsCount;
    private long collectionsCount;
    private long reviewsCount;
    private long commentsCount;
    private long favoritesCount;

    public UserProfileResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isAdmin() { return admin; }
    public void setAdmin(boolean admin) { this.admin = admin; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getMemberSince() { return memberSince; }
    public void setMemberSince(String memberSince) { this.memberSince = memberSince; }
    public long getGamesCount() { return gamesCount; }
    public void setGamesCount(long gamesCount) { this.gamesCount = gamesCount; }
    public long getRatingsCount() { return ratingsCount; }
    public void setRatingsCount(long ratingsCount) { this.ratingsCount = ratingsCount; }
    public long getCollectionsCount() { return collectionsCount; }
    public void setCollectionsCount(long collectionsCount) { this.collectionsCount = collectionsCount; }
    public long getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(long reviewsCount) { this.reviewsCount = reviewsCount; }
    public long getCommentsCount() { return commentsCount; }
    public void setCommentsCount(long commentsCount) { this.commentsCount = commentsCount; }
    public long getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(long favoritesCount) { this.favoritesCount = favoritesCount; }
}
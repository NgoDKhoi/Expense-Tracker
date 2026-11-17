package com.example.expensetracker.core.domain.model;

public class LeaderboardProfileModel {
    private final String userId;
    private final String displayName;
    private final String username;
    private final int streakDays;
    private final int totalSafeDays;
    private final String stage;

    public LeaderboardProfileModel(String userId,
                                   String displayName,
                                   String username,
                                   int streakDays,
                                   int totalSafeDays,
                                   String stage) {
        this.userId = userId;
        this.displayName = displayName;
        this.username = username;
        this.streakDays = streakDays;
        this.totalSafeDays = totalSafeDays;
        this.stage = stage;
    }

    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }
    public int getStreakDays() { return streakDays; }
    public int getTotalSafeDays() { return totalSafeDays; }
    public String getStage() { return stage; }
}

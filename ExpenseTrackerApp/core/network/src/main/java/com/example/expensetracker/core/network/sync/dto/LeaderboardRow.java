package com.example.expensetracker.core.network.sync.dto;

import com.google.gson.annotations.SerializedName;

public class LeaderboardRow {
    @SerializedName("user_id")
    public String userId;
    @SerializedName("display_name")
    public String displayName;
    @SerializedName("username")
    public String username;
    @SerializedName("streak_days")
    public int streakDays;
    @SerializedName("total_safe_days")
    public int totalSafeDays;
    @SerializedName("stage")
    public String stage;

    public LeaderboardRow() {
    }

    public LeaderboardRow(String displayName, String username, int streakDays, int totalSafeDays, String stage) {
        this.displayName = displayName;
        this.username = username;
        this.streakDays = streakDays;
        this.totalSafeDays = totalSafeDays;
        this.stage = stage;
    }
}

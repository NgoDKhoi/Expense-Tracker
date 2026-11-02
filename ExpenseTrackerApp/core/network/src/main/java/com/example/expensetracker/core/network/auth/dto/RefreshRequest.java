package com.example.expensetracker.core.network.auth.dto;

import com.google.gson.annotations.SerializedName;

public class RefreshRequest {
    @SerializedName("refresh_token")
    final String refreshToken;

    public RefreshRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

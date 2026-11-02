package com.example.expensetracker.core.network.auth.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Session returned by sign-in, and by sign-up when email confirmation is off.
 * With confirmation on, sign-up returns only the user, so accessToken is null.
 */
public class AuthResponse {
    @SerializedName("access_token")
    public String accessToken;

    @SerializedName("refresh_token")
    public String refreshToken;

    @SerializedName("expires_at")
    public long expiresAt;

    public User user;

    public static class User {
        public String id;
        public String email;

        @SerializedName("user_metadata")
        public Metadata userMetadata;
    }

    public static class Metadata {
        @SerializedName("display_name")
        public String displayName;

        public String username;
    }
}

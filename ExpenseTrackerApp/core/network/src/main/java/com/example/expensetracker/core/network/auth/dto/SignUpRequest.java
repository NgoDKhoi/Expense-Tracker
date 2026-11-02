package com.example.expensetracker.core.network.auth.dto;

import com.google.gson.annotations.SerializedName;

public class SignUpRequest {
    final String email;
    final String password;
    // Stored by Supabase as user_metadata.
    final Data data;

    public SignUpRequest(String email, String password, String displayName, String username) {
        this.email = email;
        this.password = password;
        this.data = new Data(displayName, username);
    }

    static class Data {
        @SerializedName("display_name")
        final String displayName;
        // The handle_new_user trigger copies this into public.profiles.
        final String username;

        Data(String displayName, String username) {
            this.displayName = displayName;
            this.username = username;
        }
    }
}

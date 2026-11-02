package com.example.expensetracker.core.network.auth.dto;

import com.google.gson.annotations.SerializedName;

public class UsernameRequest {
    // Parameter name of public.email_for_username in supabase/username_login.sql.
    @SerializedName("p_username")
    final String username;

    public UsernameRequest(String username) {
        this.username = username;
    }
}

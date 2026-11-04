package com.example.expensetracker.core.network.auth.dto;

public class ResendRequest {
    final String type = "signup";
    final String email;

    public ResendRequest(String email) {
        this.email = email;
    }
}

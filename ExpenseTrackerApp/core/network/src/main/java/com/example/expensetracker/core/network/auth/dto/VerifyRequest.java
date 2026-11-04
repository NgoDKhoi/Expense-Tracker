package com.example.expensetracker.core.network.auth.dto;

public class VerifyRequest {
    /** "signup" or "recovery". */
    final String type;
    final String email;
    final String token;

    public VerifyRequest(String type, String email, String token) {
        this.type = type;
        this.email = email;
        this.token = token;
    }
}

package com.example.expensetracker.core.network.auth.dto;

public class SignInRequest {
    final String email;
    final String password;

    public SignInRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}

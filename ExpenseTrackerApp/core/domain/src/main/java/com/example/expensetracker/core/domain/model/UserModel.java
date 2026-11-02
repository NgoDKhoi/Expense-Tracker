package com.example.expensetracker.core.domain.model;

public class UserModel {
    private final String id;
    private final String email;
    private final String displayName;

    public UserModel(String id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
}

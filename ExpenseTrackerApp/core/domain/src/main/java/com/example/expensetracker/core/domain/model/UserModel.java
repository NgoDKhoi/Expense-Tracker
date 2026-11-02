package com.example.expensetracker.core.domain.model;

public class UserModel {
    private final String id;
    private final String email;
    private final String displayName;
    private final String username;

    public UserModel(String id, String email, String displayName, String username) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.username = username;
    }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    /** Empty for accounts created before usernames existed. */
    public String getUsername() { return username; }
}

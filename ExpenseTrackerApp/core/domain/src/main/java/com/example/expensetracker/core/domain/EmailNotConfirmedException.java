package com.example.expensetracker.core.domain;

/**
 * Sent inside {@link Result#error} when the account exists but its email is not confirmed yet:
 * the user must enter the code mailed to {@link #getEmail()}. Not a failure to show as-is.
 */
public class EmailNotConfirmedException extends Exception {
    private final String email;

    public EmailNotConfirmedException(String email) {
        super("Email not confirmed: " + email);
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

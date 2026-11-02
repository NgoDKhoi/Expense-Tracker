package com.example.expensetracker.core.domain.usecase;

import java.util.regex.Pattern;

/** Input checks shared by the auth use cases. Returns an error message, or null when valid. */
final class AuthInput {
    // Supabase's default minimum password length.
    static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private AuthInput() {
    }

    static String checkEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "Vui lòng nhập email";
        if (!EMAIL.matcher(email.trim()).matches()) return "Email không hợp lệ";
        return null;
    }

    static String checkPassword(String password) {
        if (password == null || password.isEmpty()) return "Vui lòng nhập mật khẩu";
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự";
        }
        return null;
    }
}

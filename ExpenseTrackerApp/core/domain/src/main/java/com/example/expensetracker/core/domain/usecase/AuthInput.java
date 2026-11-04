package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.repository.IAuthRepository;

import java.util.Locale;
import java.util.regex.Pattern;

/** Input checks shared by the auth use cases. The check* methods return an error message, or null when valid. */
final class AuthInput {
    // Supabase's default minimum password length.
    static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    // Must match the username_format constraint in supabase/username_login.sql.
    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,20}$");
    // Supabase's email code is 6 digits by default; the dashboard allows up to 10.
    private static final Pattern CODE = Pattern.compile("^[0-9]{6,10}$");

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

    static String checkCode(String code) {
        if (code == null || code.trim().isEmpty()) return "Vui lòng nhập mã xác nhận";
        if (!CODE.matcher(code.trim()).matches()) return "Mã xác nhận không hợp lệ";
        return null;
    }

    static String checkUsername(String username) {
        if (username == null || username.trim().isEmpty()) return "Vui lòng nhập tên đăng nhập";
        if (!USERNAME.matcher(normalizeUsername(username)).matches()) {
            return "Tên đăng nhập gồm 3–20 ký tự: chữ không dấu, số hoặc dấu _";
        }
        return null;
    }

    /** Usernames are stored lower case, so "VietHoang" and "viethoang" are the same account. */
    static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /** Returns the email as typed, or the email registered for a username. */
    static Result<String> resolveEmail(IAuthRepository repository, String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            return Result.error("Vui lòng nhập email hoặc tên đăng nhập", null);
        }
        String input = emailOrUsername.trim();
        if (input.contains("@")) {
            String error = checkEmail(input);
            return error != null ? Result.error(error, null) : Result.success(input);
        }
        String error = checkUsername(input);
        if (error != null) return Result.error(error, null);
        Result<String> found = repository.findEmailByUsername(normalizeUsername(input));
        if (found.isError()) return found;
        if (found.getData() == null) return Result.error("Không tìm thấy tài khoản có tên đăng nhập này", null);
        return found;
    }
}

package com.example.expensetracker.core.network.auth;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Turns Supabase Auth error responses into short Vietnamese messages for the UI. */
public final class AuthErrorMapper {

    public static final String NETWORK = "Không có kết nối mạng. Vui lòng thử lại.";
    public static final String NOT_CONFIGURED = "Chưa cấu hình Supabase (thiếu SUPABASE_URL / SUPABASE_ANON_KEY trong local.properties)";

    private static final Gson GSON = new Gson();

    private AuthErrorMapper() {
    }

    /**
     * Supabase returns either {"error_code": "...", "msg": "..."} (newer)
     * or {"error": "...", "error_description": "..."} (older).
     * The database REST API (PostgREST) uses {"code": "PGRST...", "message": "..."}.
     */
    public static String fromResponse(int httpCode, String body) {
        String code = null;
        String message = null;
        try {
            JsonObject json = GSON.fromJson(body, JsonObject.class);
            if (json != null) {
                code = firstString(json, "error_code", "error", "code");
                message = firstString(json, "msg", "error_description", "message");
            }
        } catch (RuntimeException ignored) {
            // Body was not JSON; fall back to the HTTP code.
        }

        if (code != null) {
            switch (code) {
                case "invalid_credentials":
                case "invalid_grant":
                    return "Sai email hoặc mật khẩu";
                case "user_already_exists":
                case "email_exists":
                    return "Email đã được đăng ký";
                case "weak_password":
                    return "Mật khẩu quá yếu, hãy dùng mật khẩu dài hơn";
                case "email_not_confirmed":
                    return "Email chưa được xác nhận";
                case "validation_failed":
                case "email_address_invalid":
                    return "Email không hợp lệ";
                case "signup_disabled":
                    return "Chức năng đăng ký đang bị tắt";
                case "PGRST202":
                    return "Chưa cài đặt đăng nhập bằng tên trên Supabase (chạy file supabase/username_login.sql)";
                case "over_email_send_rate_limit":
                case "over_request_rate_limit":
                    return "Thao tác quá nhiều lần, vui lòng thử lại sau ít phút";
                default:
                    break;
            }
        }
        if (httpCode == 429) return "Thao tác quá nhiều lần, vui lòng thử lại sau ít phút";
        if (httpCode >= 500) return "Máy chủ đang gặp sự cố, vui lòng thử lại sau";
        return message != null ? message : "Có lỗi xảy ra (mã " + httpCode + ")";
    }

    private static String firstString(JsonObject json, String... keys) {
        for (String key : keys) {
            JsonElement value = json.get(key);
            if (value != null && value.isJsonPrimitive()) return value.getAsString();
        }
        return null;
    }
}

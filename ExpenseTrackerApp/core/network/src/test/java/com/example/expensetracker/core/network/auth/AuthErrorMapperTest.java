package com.example.expensetracker.core.network.auth;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthErrorMapperTest {

    @Test
    public void newFormat_invalidCredentials() {
        String body = "{\"code\":400,\"error_code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}";
        assertEquals("Sai email hoặc mật khẩu", AuthErrorMapper.fromResponse(400, body));
    }

    @Test
    public void oldFormat_invalidGrant() {
        String body = "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid login credentials\"}";
        assertEquals("Sai email hoặc mật khẩu", AuthErrorMapper.fromResponse(400, body));
    }

    @Test
    public void userAlreadyExists() {
        String body = "{\"code\":422,\"error_code\":\"user_already_exists\",\"msg\":\"User already registered\"}";
        assertEquals("Email đã được đăng ký", AuthErrorMapper.fromResponse(422, body));
    }

    @Test
    public void unknownCode_fallsBackToServerMessage() {
        String body = "{\"error_code\":\"something_new\",\"msg\":\"Some new error\"}";
        assertEquals("Some new error", AuthErrorMapper.fromResponse(400, body));
    }

    @Test
    public void missingUsernameFunction_tellsToRunSql() {
        String body = "{\"code\":\"PGRST202\",\"message\":\"Could not find the function public.email_for_username\"}";
        assertEquals("Chưa cài đặt đăng nhập bằng tên trên Supabase (chạy file supabase/username_login.sql)",
                AuthErrorMapper.fromResponse(404, body));
    }

    @Test
    public void rateLimitWithoutBody() {
        assertEquals("Thao tác quá nhiều lần, vui lòng thử lại sau ít phút",
                AuthErrorMapper.fromResponse(429, null));
    }

    @Test
    public void nonJsonServerError() {
        assertEquals("Máy chủ đang gặp sự cố, vui lòng thử lại sau",
                AuthErrorMapper.fromResponse(502, "<html>Bad gateway</html>"));
    }
}

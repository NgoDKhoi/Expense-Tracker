package com.example.expensetracker.core.network.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.auth.dto.AuthResponse;
import com.example.expensetracker.core.network.auth.dto.EmailRequest;
import com.example.expensetracker.core.network.auth.dto.SignInRequest;
import com.example.expensetracker.core.network.auth.dto.SignUpRequest;
import com.example.expensetracker.core.network.auth.dto.UsernameRequest;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Supabase Auth over REST. The session is kept in a private SharedPreferences file.
 * shortcut: the access token is never refreshed (expires after ~1h); add a
 * grant_type=refresh_token call when sync starts sending the token to the server.
 */
@Singleton
public class AuthRepositoryImpl implements IAuthRepository {

    private static final String PREFS = "auth_session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_EXPIRES_AT = "expires_at";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_USERNAME = "username";

    private final SupabaseAuthApi api;
    private final SharedPreferences prefs;

    @Inject
    public AuthRepositoryImpl(Context context, SupabaseAuthApi api) {
        this.api = api;
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public Result<UserModel> signUp(String email, String password, String displayName, String username) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<AuthResponse> response = api.signUp(new SignUpRequest(email, password, displayName, username)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            AuthResponse body = response.body();
            if (body == null || body.accessToken == null) {
                // Email confirmation is still on in the Supabase project.
                return Result.error("Vui lòng mở email để xác nhận tài khoản, sau đó đăng nhập", null);
            }
            return Result.success(saveSession(body));
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<UserModel> signIn(String email, String password) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<AuthResponse> response = api.signIn(new SignInRequest(email, password)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            AuthResponse body = response.body();
            if (body == null || body.accessToken == null || body.user == null) {
                return Result.error("Phản hồi đăng nhập không hợp lệ", null);
            }
            return Result.success(saveSession(body));
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<Void> sendPasswordReset(String email) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<Void> response = api.recover(new EmailRequest(email)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<String> findEmailByUsername(String username) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<String> response = api.emailForUsername(new UsernameRequest(username)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            // A JSON null body means no account uses this username.
            return Result.success(response.body());
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public void signOut() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        // Clear locally first so the user is signed out even when offline.
        prefs.edit().clear().apply();
        if (token == null || !isConfigured()) return;
        try {
            api.logout("Bearer " + token).execute();
        } catch (IOException ignored) {
            // The server session expires on its own.
        }
    }

    @Override
    public UserModel getCurrentUser() {
        String id = prefs.getString(KEY_USER_ID, null);
        if (id == null) return null;
        return new UserModel(id, prefs.getString(KEY_EMAIL, ""), prefs.getString(KEY_DISPLAY_NAME, ""),
                prefs.getString(KEY_USERNAME, ""));
    }

    private UserModel saveSession(AuthResponse body) {
        AuthResponse.Metadata meta = body.user.userMetadata;
        String displayName = meta != null && meta.displayName != null ? meta.displayName : "";
        String username = meta != null && meta.username != null ? meta.username : "";
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, body.accessToken)
                .putString(KEY_REFRESH_TOKEN, body.refreshToken)
                .putLong(KEY_EXPIRES_AT, body.expiresAt)
                .putString(KEY_USER_ID, body.user.id)
                .putString(KEY_EMAIL, body.user.email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_USERNAME, username)
                .apply();
        return new UserModel(body.user.id, body.user.email, displayName, username);
    }

    private static String errorMessage(Response<?> response) {
        String body = null;
        try (ResponseBody errorBody = response.errorBody()) {
            if (errorBody != null) body = errorBody.string();
        } catch (IOException ignored) {
            // Use the HTTP code only.
        }
        return AuthErrorMapper.fromResponse(response.code(), body);
    }

    private static boolean isConfigured() {
        return !BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_ANON_KEY.isEmpty();
    }
}

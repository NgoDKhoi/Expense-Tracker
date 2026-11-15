package com.example.expensetracker.core.network.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.expensetracker.core.domain.EmailNotConfirmedException;
import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.auth.dto.AuthResponse;
import com.example.expensetracker.core.network.auth.dto.EmailRequest;
import com.example.expensetracker.core.network.auth.dto.PasswordRequest;
import com.example.expensetracker.core.network.auth.dto.RefreshRequest;
import com.example.expensetracker.core.network.auth.dto.ResendRequest;
import com.example.expensetracker.core.network.auth.dto.SignInRequest;
import com.example.expensetracker.core.network.auth.dto.SignUpRequest;
import com.example.expensetracker.core.network.auth.dto.UsernameRequest;
import com.example.expensetracker.core.network.auth.dto.VerifyRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.LongSupplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Supabase Auth over REST. The session is kept in a private SharedPreferences file.
 * The access token lasts about an hour; getValidAccessToken() refreshes it before use.
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

    // Refresh when the token has less than this left, so it can't expire mid-request.
    private static final long REFRESH_MARGIN_MS = 60_000;

    private final SupabaseAuthApi api;
    private final SharedPreferences prefs;
    private final LongSupplier clock;

    @Inject
    public AuthRepositoryImpl(Context context, SupabaseAuthApi api) {
        this(context, api, System::currentTimeMillis);
    }

    AuthRepositoryImpl(Context context, SupabaseAuthApi api, LongSupplier clock) {
        this.api = api;
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.clock = clock;
    }

    @Override
    public Result<UserModel> signUp(String email, String password, String displayName, String username) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<AuthResponse> response = api.signUp(new SignUpRequest(email, password, displayName, username)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            AuthResponse body = response.body();
            if (body == null || body.accessToken == null) {
                // Confirm email is on in the Supabase project: a code was mailed instead of a session.
                return codeNeeded(email);
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
            if (!response.isSuccessful()) {
                String body = errorBody(response);
                if (body != null && body.contains("email_not_confirmed")) return codeNeeded(email);
                return Result.error(AuthErrorMapper.fromResponse(response.code(), body), null);
            }
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

    // synchronized: Supabase refresh tokens are single-use, so two parallel refreshes would sign the user out.
    @Override
    public synchronized Result<String> getValidAccessToken() {
        String token = prefs.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null);
        if (token == null || refreshToken == null) return Result.success(null);
        long expiresAtMs = prefs.getLong(KEY_EXPIRES_AT, 0) * 1000;
        if (expiresAtMs - clock.getAsLong() > REFRESH_MARGIN_MS) return Result.success(token);
        try {
            Response<AuthResponse> response = api.refresh(new RefreshRequest(refreshToken)).execute();
            if (!response.isSuccessful()) {
                // Server trouble or rate limit: keep the session and retry later.
                if (response.code() >= 500 || response.code() == 429) return Result.error(errorMessage(response), null);
                // The session was revoked or the refresh token is no longer valid: treat as signed out.
                prefs.edit().clear().apply();
                return Result.success(null);
            }
            AuthResponse body = response.body();
            if (body == null || body.accessToken == null || body.user == null) {
                return Result.error("Phản hồi làm mới phiên không hợp lệ", null);
            }
            saveSession(body);
            return Result.success(body.accessToken);
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<UserModel> verifyEmailCode(String email, String code, boolean recovery) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<AuthResponse> response = api.verify(
                    new VerifyRequest(recovery ? "recovery" : "signup", email, code)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            AuthResponse body = response.body();
            if (body == null || body.accessToken == null || body.user == null) {
                return Result.error(AuthErrorMapper.CODE_INVALID, null);
            }
            return Result.success(saveSession(body));
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<Void> resendSignUpCode(String email) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        try {
            Response<Void> response = api.resend(new ResendRequest(email)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<Void> updatePassword(String newPassword) {
        Result<String> token = getValidAccessToken();
        if (!token.isSuccess()) return Result.error(token.getMessage(), token.getException());
        if (token.getData() == null) return Result.error("Phiên đăng nhập đã hết, vui lòng thử lại", null);
        try {
            Response<Void> response = api.updateUser("Bearer " + token.getData(), new PasswordRequest(newPassword)).execute();
            if (!response.isSuccessful()) return Result.error(errorMessage(response), null);
            return Result.success(null);
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    @Override
    public Result<String> getGoogleSignInUrl(String redirectUri) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        if (isBlank(redirectUri)) return Result.error("Thiếu địa chỉ chuyển hướng xác thực", null);
        String base = BuildConfig.SUPABASE_URL.endsWith("/")
                ? BuildConfig.SUPABASE_URL
                : BuildConfig.SUPABASE_URL + "/";
        String encodedRedirect = urlEncode(redirectUri.trim());
        return Result.success(base + "auth/v1/authorize?provider=google&redirect_to="
                + encodedRedirect + "&prompt=select_account");
    }

    @Override
    public Result<UserModel> completeOAuthSignIn(String callbackUri) {
        if (!isConfigured()) return Result.error(AuthErrorMapper.NOT_CONFIGURED, null);
        if (isBlank(callbackUri)) return Result.error("Không nhận được phản hồi đăng nhập từ Google", null);

        Map<String, String> params = parseCallbackParams(callbackUri);
        String errorDescription = params.get("error_description");
        String error = params.get("error");
        if (!isBlank(errorDescription)) {
            return Result.error(errorDescription.trim(), null);
        }
        if (!isBlank(error)) {
            return Result.error("Đăng nhập Google thất bại: " + error.trim(), null);
        }

        String accessToken = params.get("access_token");
        String refreshToken = params.get("refresh_token");
        if (isBlank(accessToken) || isBlank(refreshToken)) {
            return Result.error("Không nhận được phiên đăng nhập từ Google", null);
        }

        long expiresAt = resolveExpiresAt(params, clock.getAsLong());
        try {
            Response<AuthResponse.User> userResponse = api.getUser("Bearer " + accessToken).execute();
            if (!userResponse.isSuccessful()) {
                return Result.error(errorMessage(userResponse), null);
            }
            AuthResponse.User user = userResponse.body();
            if (user == null || isBlank(user.id)) {
                return Result.error("Phản hồi người dùng không hợp lệ", null);
            }
            AuthResponse body = new AuthResponse();
            body.accessToken = accessToken;
            body.refreshToken = refreshToken;
            body.expiresAt = expiresAt;
            body.user = user;
            return Result.success(saveSession(body));
        } catch (IOException e) {
            return Result.error(AuthErrorMapper.NETWORK, e);
        }
    }

    private static <T> Result<T> codeNeeded(String email) {
        return Result.error("Nhập mã xác nhận đã gửi tới " + email, new EmailNotConfirmedException(email));
    }

    private UserModel saveSession(AuthResponse body) {
        AuthResponse.Metadata meta = body.user.userMetadata;
        String email = body.user.email != null ? body.user.email : "";
        String displayName = resolveDisplayName(meta, email);
        String username = resolveUsername(meta, email);
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, body.accessToken)
                .putString(KEY_REFRESH_TOKEN, body.refreshToken)
                .putLong(KEY_EXPIRES_AT, body.expiresAt)
                .putString(KEY_USER_ID, body.user.id)
                .putString(KEY_EMAIL, email)
                .putString(KEY_DISPLAY_NAME, displayName)
                .putString(KEY_USERNAME, username)
                .apply();
        return new UserModel(body.user.id, email, displayName, username);
    }

    private static String resolveDisplayName(AuthResponse.Metadata meta, String email) {
        if (meta != null) {
            if (!isBlank(meta.displayName)) return meta.displayName.trim();
            if (!isBlank(meta.fullName)) return meta.fullName.trim();
            if (!isBlank(meta.name)) return meta.name.trim();
        }
        return emailLocalPart(email);
    }

    private static String resolveUsername(AuthResponse.Metadata meta, String email) {
        if (meta != null && !isBlank(meta.username)) {
            return meta.username.trim();
        }
        return deriveUsernameFromEmail(email);
    }

    static String deriveUsernameFromEmail(String email) {
        String local = emailLocalPart(email).toLowerCase(Locale.ROOT);
        if (local.isEmpty()) return "";
        String normalized = local.replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.length() > 20) {
            return normalized.substring(0, 20);
        }
        return normalized;
    }

    private static String emailLocalPart(String email) {
        if (isBlank(email)) return "";
        int at = email.indexOf('@');
        return (at > 0 ? email.substring(0, at) : email).trim();
    }

    static Map<String, String> parseCallbackParams(String callbackUri) {
        Map<String, String> params = new HashMap<>();
        if (callbackUri == null) return params;

        int hashIndex = callbackUri.indexOf('#');
        int queryIndex = callbackUri.indexOf('?');

        if (queryIndex >= 0 && (hashIndex < 0 || queryIndex < hashIndex)) {
            String query = hashIndex >= 0
                    ? callbackUri.substring(queryIndex + 1, hashIndex)
                    : callbackUri.substring(queryIndex + 1);
            collectEncodedPairs(query, params);
        }
        if (hashIndex >= 0 && hashIndex + 1 < callbackUri.length()) {
            collectEncodedPairs(callbackUri.substring(hashIndex + 1), params);
        }
        return params;
    }

    private static void collectEncodedPairs(String raw, Map<String, String> out) {
        if (raw == null || raw.isEmpty()) return;
        for (String pair : raw.split("&")) {
            if (pair.isEmpty()) continue;
            int eq = pair.indexOf('=');
            String key = urlDecode(eq >= 0 ? pair.substring(0, eq) : pair);
            String value = eq >= 0 ? urlDecode(pair.substring(eq + 1)) : "";
            if (!key.isEmpty()) {
                out.put(key, value);
            }
        }
    }

    private static long resolveExpiresAt(Map<String, String> params, long nowMillis) {
        long nowSeconds = nowMillis / 1000;
        String expiresAtStr = params.get("expires_at");
        if (!isBlank(expiresAtStr)) {
            try {
                return Long.parseLong(expiresAtStr.trim());
            } catch (NumberFormatException ignored) {
                // Fall back to expires_in.
            }
        }
        String expiresInStr = params.get("expires_in");
        if (!isBlank(expiresInStr)) {
            try {
                return nowSeconds + Long.parseLong(expiresInStr.trim());
            } catch (NumberFormatException ignored) {
                // Fall back to default TTL.
            }
        }
        return nowSeconds + 3600;
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unavailable", e);
        }
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            return value;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String errorMessage(Response<?> response) {
        return AuthErrorMapper.fromResponse(response.code(), errorBody(response));
    }

    private static String errorBody(Response<?> response) {
        try (ResponseBody errorBody = response.errorBody()) {
            return errorBody != null ? errorBody.string() : null;
        } catch (IOException ignored) {
            return null; // Use the HTTP code only.
        }
    }

    private static boolean isConfigured() {
        return !BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_ANON_KEY.isEmpty();
    }
}

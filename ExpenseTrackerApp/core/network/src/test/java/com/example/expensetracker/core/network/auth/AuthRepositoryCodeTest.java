package com.example.expensetracker.core.network.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.example.expensetracker.core.domain.EmailNotConfirmedException;
import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.auth.dto.AuthResponse;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class AuthRepositoryCodeTest {
    private static final long NOW = 1_000_000_000_000L;

    private SupabaseAuthApi api;
    private FakePrefs prefs;
    private AuthRepositoryImpl repo;

    @Before
    public void setup() {
        api = mock(SupabaseAuthApi.class);
        prefs = new FakePrefs();
        Context context = mock(Context.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        repo = new AuthRepositoryImpl(context, api, () -> NOW);
    }

    // Calls that check BuildConfig need Supabase keys in local.properties.
    private static void assumeConfigured() {
        assumeTrue(!BuildConfig.SUPABASE_URL.isEmpty() && !BuildConfig.SUPABASE_ANON_KEY.isEmpty());
    }

    private static AuthResponse session(String token) {
        AuthResponse body = new AuthResponse();
        body.accessToken = token;
        body.refreshToken = "r";
        body.expiresAt = NOW / 1000 + 3600;
        body.user = new AuthResponse.User();
        body.user.id = "u1";
        body.user.email = "a@b.com";
        return body;
    }

    @SuppressWarnings("unchecked")
    private static <T> Call<T> call(Response<T> response) throws IOException {
        Call<T> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }

    @Test
    public void signUpWithoutSession_asksForCode() throws IOException {
        assumeConfigured();
        AuthResponse noSession = new AuthResponse();
        Call<AuthResponse> c = call(Response.success(noSession));
        when(api.signUp(any())).thenReturn(c);

        Result<UserModel> result = repo.signUp("a@b.com", "123456", "An", "an");

        assertTrue(result.isError());
        assertEquals("a@b.com", ((EmailNotConfirmedException) result.getException()).getEmail());
    }

    @Test
    public void signInUnconfirmed_asksForCode() throws IOException {
        assumeConfigured();
        Call<AuthResponse> c = call(Response.error(400,
                ResponseBody.create(null, "{\"error_code\":\"email_not_confirmed\",\"msg\":\"Email not confirmed\"}")));
        when(api.signIn(any())).thenReturn(c);

        Result<UserModel> result = repo.signIn("a@b.com", "123456");

        assertTrue(result.getException() instanceof EmailNotConfirmedException);
    }

    @Test
    public void verifyCode_savesSession() throws IOException {
        assumeConfigured();
        Call<AuthResponse> c = call(Response.success(session("tok")));
        when(api.verify(any())).thenReturn(c);

        assertTrue(repo.verifyEmailCode("a@b.com", "123456", false).isSuccess());
        assertEquals("a@b.com", repo.getCurrentUser().getEmail());
        assertEquals("tok", repo.getValidAccessToken().getData());
    }

    @Test
    public void updatePassword_sendsBearerToken() throws IOException {
        prefs.edit().putString("access_token", "tok").putString("refresh_token", "r")
                .putLong("expires_at", NOW / 1000 + 3600).putString("user_id", "u1").apply();
        Call<Void> c = call(Response.success(null));
        when(api.updateUser(anyString(), any())).thenReturn(c);

        assertTrue(repo.updatePassword("newpass").isSuccess());
        verify(api).updateUser(eq("Bearer tok"), any());
    }

    @Test
    public void updatePassword_signedOut_fails() {
        assertTrue(repo.updatePassword("newpass").isError());
    }
}

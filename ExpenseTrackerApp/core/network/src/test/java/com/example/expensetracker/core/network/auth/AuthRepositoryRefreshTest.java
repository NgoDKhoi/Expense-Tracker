package com.example.expensetracker.core.network.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.network.auth.dto.AuthResponse;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class AuthRepositoryRefreshTest {
    private static final long NOW = 1_000_000_000_000L;

    private SupabaseAuthApi api;
    private Call<AuthResponse> call;
    private FakePrefs prefs;
    private AuthRepositoryImpl repo;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        api = mock(SupabaseAuthApi.class);
        call = mock(Call.class);
        when(api.refresh(any())).thenReturn(call);
        prefs = new FakePrefs();
        Context context = mock(Context.class);
        when(context.getSharedPreferences(anyString(), anyInt())).thenReturn(prefs);
        repo = new AuthRepositoryImpl(context, api, () -> NOW);
    }

    private void signedIn(long expiresAtSeconds) {
        prefs.edit().putString("access_token", "old").putString("refresh_token", "r1")
                .putLong("expires_at", expiresAtSeconds).putString("user_id", "u1").apply();
    }

    @Test
    public void signedOut_returnsNullWithoutCallingServer() {
        Result<String> result = repo.getValidAccessToken();
        assertTrue(result.isSuccess());
        assertNull(result.getData());
        verify(api, never()).refresh(any());
    }

    @Test
    public void freshToken_isReturnedAsIs() {
        signedIn(NOW / 1000 + 3600);
        assertEquals("old", repo.getValidAccessToken().getData());
        verify(api, never()).refresh(any());
    }

    @Test
    public void nearlyExpiredToken_isRefreshedAndSaved() throws IOException {
        signedIn(NOW / 1000 + 30);
        AuthResponse body = new AuthResponse();
        body.accessToken = "new";
        body.refreshToken = "r2";
        body.expiresAt = NOW / 1000 + 3600;
        body.user = new AuthResponse.User();
        body.user.id = "u1";
        body.user.email = "a@b.c";
        when(call.execute()).thenReturn(Response.success(body));

        assertEquals("new", repo.getValidAccessToken().getData());
        assertEquals("r2", prefs.getString("refresh_token", null));
        // Now fresh, so no second refresh.
        assertEquals("new", repo.getValidAccessToken().getData());
        verify(api, org.mockito.Mockito.times(1)).refresh(any());
    }

    @Test
    public void rejectedRefresh_signsOut() throws IOException {
        signedIn(NOW / 1000 - 10);
        when(call.execute()).thenReturn(Response.error(400,
                ResponseBody.create(null, "{\"error_code\":\"refresh_token_not_found\"}")));

        Result<String> result = repo.getValidAccessToken();
        assertTrue(result.isSuccess());
        assertNull(result.getData());
        assertNull(prefs.getString("user_id", null));
    }

    @Test
    public void networkError_keepsSession() throws IOException {
        signedIn(NOW / 1000 - 10);
        when(call.execute()).thenThrow(new IOException("offline"));

        assertFalse(repo.getValidAccessToken().isSuccess());
        assertEquals("r1", prefs.getString("refresh_token", null));
    }

    @Test
    public void serverError_keepsSession() throws IOException {
        signedIn(NOW / 1000 - 10);
        when(call.execute()).thenReturn(Response.error(503, ResponseBody.create(null, "")));

        assertFalse(repo.getValidAccessToken().isSuccess());
        assertEquals("r1", prefs.getString("refresh_token", null));
    }
}

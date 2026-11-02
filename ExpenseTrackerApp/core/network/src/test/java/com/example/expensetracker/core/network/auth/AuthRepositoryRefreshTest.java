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
import android.content.SharedPreferences;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.network.auth.dto.AuthResponse;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

    /** In-memory SharedPreferences; only what AuthRepositoryImpl uses. */
    private static class FakePrefs implements SharedPreferences {
        final Map<String, Object> map = new HashMap<>();

        @Override public Map<String, ?> getAll() { return map; }
        @Override public String getString(String k, String d) { return map.containsKey(k) ? (String) map.get(k) : d; }
        @Override public Set<String> getStringSet(String k, Set<String> d) { throw new UnsupportedOperationException(); }
        @Override public int getInt(String k, int d) { throw new UnsupportedOperationException(); }
        @Override public long getLong(String k, long d) { return map.containsKey(k) ? (Long) map.get(k) : d; }
        @Override public float getFloat(String k, float d) { throw new UnsupportedOperationException(); }
        @Override public boolean getBoolean(String k, boolean d) { throw new UnsupportedOperationException(); }
        @Override public boolean contains(String k) { return map.containsKey(k); }
        @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { }

        @Override
        public Editor edit() {
            return new Editor() {
                final Map<String, Object> pending = new HashMap<>();
                boolean clear;

                @Override public Editor putString(String k, String v) { pending.put(k, v); return this; }
                @Override public Editor putStringSet(String k, Set<String> v) { throw new UnsupportedOperationException(); }
                @Override public Editor putInt(String k, int v) { throw new UnsupportedOperationException(); }
                @Override public Editor putLong(String k, long v) { pending.put(k, v); return this; }
                @Override public Editor putFloat(String k, float v) { throw new UnsupportedOperationException(); }
                @Override public Editor putBoolean(String k, boolean v) { throw new UnsupportedOperationException(); }
                @Override public Editor remove(String k) { pending.put(k, null); return this; }
                @Override public Editor clear() { clear = true; return this; }
                @Override public boolean commit() { apply(); return true; }

                @Override
                public void apply() {
                    if (clear) map.clear();
                    for (Map.Entry<String, Object> e : pending.entrySet()) {
                        if (e.getValue() == null) map.remove(e.getKey()); else map.put(e.getKey(), e.getValue());
                    }
                }
            };
        }
    }
}

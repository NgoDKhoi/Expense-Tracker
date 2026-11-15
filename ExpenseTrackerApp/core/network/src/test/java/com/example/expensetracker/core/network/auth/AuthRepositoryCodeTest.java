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
import java.util.Map;

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

    @Test
    public void getGoogleSignInUrl_buildsAuthorizeUrlWithGoogleProvider() {
        assumeConfigured();

        Result<String> result = repo.getGoogleSignInUrl("expensetracker://auth-callback");

        assertTrue(result.isSuccess());
        assertTrue(result.getData().contains("auth/v1/authorize?provider=google"));
        assertTrue(result.getData().contains("redirect_to=expensetracker%3A%2F%2Fauth-callback"));
        assertTrue(result.getData().contains("prompt=select_account"));
    }

    @Test
    public void completeOAuthSignIn_validFragment_fetchesUserAndDerivesGoogleProfile() throws IOException {
        assumeConfigured();
        AuthResponse.User googleUser = new AuthResponse.User();
        googleUser.id = "g-1";
        googleUser.email = "khoi.nguyen@gmail.com";
        googleUser.userMetadata = new AuthResponse.Metadata();
        googleUser.userMetadata.fullName = "Khoi Nguyen";

        Call<AuthResponse.User> userCall = call(Response.success(googleUser));
        when(api.getUser("Bearer access_tok")).thenReturn(userCall);

        String callback = "expensetracker://auth-callback#access_token=access_tok&refresh_token=ref_tok&expires_in=3600";
        Result<UserModel> result = repo.completeOAuthSignIn(callback);

        assertTrue(result.isSuccess());
        assertEquals("Khoi Nguyen", result.getData().getDisplayName());
        assertEquals("khoi_nguyen", result.getData().getUsername());
        assertEquals("khoi.nguyen@gmail.com", repo.getCurrentUser().getEmail());
        assertEquals("access_tok", repo.getValidAccessToken().getData());
    }

    @Test
    public void completeOAuthSignIn_errorInCallback_returnsErrorMessage() {
        assumeConfigured();

        String callback = "expensetracker://auth-callback#error=access_denied&error_description=User+cancelled+login";
        Result<UserModel> result = repo.completeOAuthSignIn(callback);

        assertTrue(result.isError());
        assertEquals("User cancelled login", result.getMessage());
    }

    @Test
    public void deriveUsernameFromEmail_normalizesAndTruncates() {
        assertEquals("khoi_nguyen", AuthRepositoryImpl.deriveUsernameFromEmail("Khoi.Nguyen+123@gmail.com".replace("+123", "")));
        assertEquals("very_long_email_addr", AuthRepositoryImpl.deriveUsernameFromEmail("very.long.email.address.here@gmail.com"));
    }

    @Test
    public void parseCallbackParams_extractsQueryAndFragmentTokens() {
        Map<String, String> params = AuthRepositoryImpl.parseCallbackParams(
                "expensetracker://auth-callback?state=abc#access_token=t1&refresh_token=r1&expires_in=3600");
        assertEquals("abc", params.get("state"));
        assertEquals("t1", params.get("access_token"));
        assertEquals("r1", params.get("refresh_token"));
        assertEquals("3600", params.get("expires_in"));
    }
}

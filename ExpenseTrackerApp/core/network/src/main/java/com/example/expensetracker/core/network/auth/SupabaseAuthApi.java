package com.example.expensetracker.core.network.auth;

import com.example.expensetracker.core.network.auth.dto.AuthResponse;
import com.example.expensetracker.core.network.auth.dto.EmailRequest;
import com.example.expensetracker.core.network.auth.dto.PasswordRequest;
import com.example.expensetracker.core.network.auth.dto.RefreshRequest;
import com.example.expensetracker.core.network.auth.dto.ResendRequest;
import com.example.expensetracker.core.network.auth.dto.SignInRequest;
import com.example.expensetracker.core.network.auth.dto.SignUpRequest;
import com.example.expensetracker.core.network.auth.dto.UsernameRequest;
import com.example.expensetracker.core.network.auth.dto.VerifyRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;

/** Supabase Auth (GoTrue) REST endpoints, plus the username lookup RPC. The apikey header is added by NetworkModule. */
public interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest body);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> signIn(@Body SignInRequest body);

    @POST("auth/v1/token?grant_type=refresh_token")
    Call<AuthResponse> refresh(@Body RefreshRequest body);

    @POST("auth/v1/recover")
    Call<Void> recover(@Body EmailRequest body);

    /** Email code (sign up or password reset) in exchange for a session. */
    @POST("auth/v1/verify")
    Call<AuthResponse> verify(@Body VerifyRequest body);

    @POST("auth/v1/resend")
    Call<Void> resend(@Body ResendRequest body);

    @PUT("auth/v1/user")
    Call<Void> updateUser(@Header("Authorization") String bearerToken, @Body PasswordRequest body);

    @POST("auth/v1/logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);

    /** SQL function from supabase/username_login.sql; returns the email as a JSON string, or null. */
    @POST("rest/v1/rpc/email_for_username")
    Call<String> emailForUsername(@Body UsernameRequest body);
}

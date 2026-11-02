package com.example.expensetracker.core.network.auth;

import com.example.expensetracker.core.network.auth.dto.AuthResponse;
import com.example.expensetracker.core.network.auth.dto.EmailRequest;
import com.example.expensetracker.core.network.auth.dto.SignInRequest;
import com.example.expensetracker.core.network.auth.dto.SignUpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/** Supabase Auth (GoTrue) REST endpoints. The apikey header is added by NetworkModule. */
public interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest body);

    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> signIn(@Body SignInRequest body);

    @POST("auth/v1/recover")
    Call<Void> recover(@Body EmailRequest body);

    @POST("auth/v1/logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);
}

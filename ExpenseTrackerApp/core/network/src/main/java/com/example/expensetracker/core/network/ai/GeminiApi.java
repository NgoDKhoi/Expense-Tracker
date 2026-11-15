package com.example.expensetracker.core.network.ai;

import com.example.expensetracker.core.network.ai.dto.GeminiRequest;
import com.example.expensetracker.core.network.ai.dto.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiApi {
    @POST("v1beta/models/gemini-2.0-flash:generateContent")
    Call<GeminiResponse> generateContent(@Query("key") String apiKey, @Body GeminiRequest request);
}

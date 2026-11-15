package com.example.expensetracker.core.network.ai;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.repository.IAiChatRepository;
import com.example.expensetracker.core.network.BuildConfig;
import com.example.expensetracker.core.network.ai.dto.GeminiRequest;
import com.example.expensetracker.core.network.ai.dto.GeminiResponse;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

@Singleton
public class GeminiChatRepositoryImpl implements IAiChatRepository {

    private final GeminiApi geminiApi;

    @Inject
    public GeminiChatRepositoryImpl(GeminiApi geminiApi) {
        this.geminiApi = geminiApi;
    }

    @Override
    public boolean isConfigured() {
        String key = BuildConfig.GEMINI_API_KEY;
        return key != null && !key.trim().isEmpty();
    }

    @Override
    public Result<String> sendPrompt(String systemInstruction, String userMessage) {
        if (!isConfigured()) {
            return Result.error("GEMINI_NOT_CONFIGURED", null);
        }
        try {
            Response<GeminiResponse> response = geminiApi
                    .generateContent(BuildConfig.GEMINI_API_KEY.trim(), new GeminiRequest(systemInstruction, userMessage))
                    .execute();
            if (!response.isSuccessful() || response.body() == null) {
                return Result.error("HTTP_" + response.code(), null);
            }
            String text = response.body().firstText();
            if (text == null || text.trim().isEmpty()) {
                return Result.error("EMPTY_RESPONSE", null);
            }
            return Result.success(text.trim());
        } catch (Exception e) {
            return Result.error("NETWORK_ERROR", e);
        }
    }
}

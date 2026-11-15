package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.Result;

/**
 * AI financial advisor chat repository (Gemini REST API implementation in :core:network).
 */
public interface IAiChatRepository {
    boolean isConfigured();
    Result<String> sendPrompt(String systemInstruction, String userMessage);
}

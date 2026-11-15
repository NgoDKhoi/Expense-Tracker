package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.repository.IAiChatRepository;

import javax.inject.Inject;

public class SendAiChatPromptUseCase {
    private final IAiChatRepository repository;

    @Inject
    public SendAiChatPromptUseCase(IAiChatRepository repository) {
        this.repository = repository;
    }

    public boolean isConfigured() {
        return repository.isConfigured();
    }

    public Result<String> execute(String systemInstruction, String userMessage) {
        return repository.sendPrompt(systemInstruction, userMessage);
    }
}

package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class ResetPasswordUseCase {
    private final IAuthRepository repository;

    @Inject
    public ResetPasswordUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(String email) {
        String error = AuthInput.checkEmail(email);
        if (error != null) return Result.error(error, null);
        return repository.sendPasswordReset(email.trim());
    }
}

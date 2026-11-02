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

    /** Accepts either an email or a username; the reset link goes to the account's email. */
    public Result<Void> execute(String emailOrUsername) {
        Result<String> email = AuthInput.resolveEmail(repository, emailOrUsername);
        if (email.isError()) return Result.error(email.getMessage(), email.getException());
        return repository.sendPasswordReset(email.getData());
    }
}

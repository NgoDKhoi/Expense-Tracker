package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;

import javax.inject.Inject;

/** Confirms a new account with the code mailed at sign up; success signs the user in. Blocks. */
public class VerifySignUpUseCase {
    private final IAuthRepository repository;

    @Inject
    public VerifySignUpUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    public Result<UserModel> execute(String email, String code) {
        String error = AuthInput.checkCode(code);
        if (error != null) return Result.error(error, null);
        return repository.verifyEmailCode(email, code.trim(), false);
    }

    public Result<Void> resend(String email) {
        return repository.resendSignUpCode(email);
    }
}

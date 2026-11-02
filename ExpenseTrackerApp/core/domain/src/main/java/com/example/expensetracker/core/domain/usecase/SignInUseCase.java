package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class SignInUseCase {
    private final IAuthRepository repository;

    @Inject
    public SignInUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    /** Accepts either an email or a username. */
    public Result<UserModel> execute(String emailOrUsername, String password) {
        if (password == null || password.isEmpty()) {
            if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
                return Result.error("Vui lòng nhập email hoặc tên đăng nhập", null);
            }
            return Result.error("Vui lòng nhập mật khẩu", null);
        }
        Result<String> email = AuthInput.resolveEmail(repository, emailOrUsername);
        if (email.isError()) return Result.error(email.getMessage(), email.getException());
        return repository.signIn(email.getData(), password);
    }
}

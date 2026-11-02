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

    public Result<UserModel> execute(String email, String password) {
        String error = AuthInput.checkEmail(email);
        if (error == null && (password == null || password.isEmpty())) {
            error = "Vui lòng nhập mật khẩu";
        }
        if (error != null) return Result.error(error, null);
        return repository.signIn(email.trim(), password);
    }
}

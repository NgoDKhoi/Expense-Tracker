package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class SignUpUseCase {
    private final IAuthRepository repository;

    @Inject
    public SignUpUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    public Result<UserModel> execute(String displayName, String email, String password) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return Result.error("Vui lòng nhập tên hiển thị", null);
        }
        String error = AuthInput.checkEmail(email);
        if (error == null) error = AuthInput.checkPassword(password);
        if (error != null) return Result.error(error, null);
        return repository.signUp(email.trim(), password, displayName.trim());
    }
}

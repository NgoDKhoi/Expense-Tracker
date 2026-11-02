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

    public Result<UserModel> execute(String displayName, String username, String email, String password) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return Result.error("Vui lòng nhập tên hiển thị", null);
        }
        String error = AuthInput.checkUsername(username);
        if (error == null) error = AuthInput.checkEmail(email);
        if (error == null) error = AuthInput.checkPassword(password);
        if (error != null) return Result.error(error, null);

        String normalizedUsername = AuthInput.normalizeUsername(username);
        Result<String> taken = repository.findEmailByUsername(normalizedUsername);
        if (taken.isError()) return Result.error(taken.getMessage(), taken.getException());
        if (taken.getData() != null) return Result.error("Tên đăng nhập đã được sử dụng", null);

        return repository.signUp(email.trim(), password, displayName.trim(), normalizedUsername);
    }
}

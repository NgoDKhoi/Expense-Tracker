package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;

import javax.inject.Inject;

/** Handles building the Google OAuth URL and completing sign-in from the deep-link callback. */
public class CompleteGoogleSignInUseCase {
    private final IAuthRepository repository;

    @Inject
    public CompleteGoogleSignInUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    public Result<String> getSignInUrl(String redirectUri) {
        if (redirectUri == null || redirectUri.trim().isEmpty()) {
            return Result.error("Thiếu địa chỉ chuyển hướng xác thực", null);
        }
        return repository.getGoogleSignInUrl(redirectUri.trim());
    }

    public Result<UserModel> execute(String callbackUri) {
        if (callbackUri == null || callbackUri.trim().isEmpty()) {
            return Result.error("Không nhận được phản hồi đăng nhập từ Google", null);
        }
        return repository.completeOAuthSignIn(callbackUri.trim());
    }
}

package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class ResetPasswordUseCase {
    private final IAuthRepository repository;

    @Inject
    public ResetPasswordUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    /** Accepts either an email or a username; returns the email the reset code was sent to. */
    public Result<String> execute(String emailOrUsername) {
        Result<String> email = AuthInput.resolveEmail(repository, emailOrUsername);
        if (email.isError()) return Result.error(email.getMessage(), email.getException());
        Result<Void> sent = repository.sendPasswordReset(email.getData());
        if (sent.isError()) return Result.error(sent.getMessage(), sent.getException());
        return Result.success(email.getData());
    }

    /** Checks the mailed code and sets the new password; success leaves the user signed in. */
    public Result<UserModel> confirm(String email, String code, String newPassword) {
        String error = AuthInput.checkCode(code);
        if (error == null) error = AuthInput.checkPassword(newPassword);
        if (error != null) return Result.error(error, null);

        UserModel user = repository.getCurrentUser();
        // Already verified on an earlier try whose password update failed: the code is used up, skip it.
        if (user == null || !email.equalsIgnoreCase(user.getEmail())) {
            Result<UserModel> verified = repository.verifyEmailCode(email, code.trim(), true);
            if (!verified.isSuccess()) return verified;
            user = verified.getData();
        }
        Result<Void> updated = repository.updatePassword(newPassword);
        if (!updated.isSuccess()) return Result.error(updated.getMessage(), updated.getException());
        return Result.success(user);
    }
}

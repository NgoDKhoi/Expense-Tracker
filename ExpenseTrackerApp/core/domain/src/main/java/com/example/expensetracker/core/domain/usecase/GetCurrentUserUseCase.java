package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.model.UserModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class GetCurrentUserUseCase {
    private final IAuthRepository repository;

    @Inject
    public GetCurrentUserUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    /** Returns the signed-in user, or null for guests. */
    public UserModel execute() {
        return repository.getCurrentUser();
    }
}

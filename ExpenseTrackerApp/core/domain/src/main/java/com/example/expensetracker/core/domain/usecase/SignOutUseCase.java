package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.repository.IAuthRepository;
import javax.inject.Inject;

public class SignOutUseCase {
    private final IAuthRepository repository;

    @Inject
    public SignOutUseCase(IAuthRepository repository) {
        this.repository = repository;
    }

    public void execute() {
        repository.signOut();
    }
}

package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.repository.IWalletRepository;

import javax.inject.Inject;

public class DeleteWalletUseCase {
    private final IWalletRepository repository;

    @Inject
    public DeleteWalletUseCase(IWalletRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(long walletId) {
        try {
            repository.deleteWallet(walletId);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

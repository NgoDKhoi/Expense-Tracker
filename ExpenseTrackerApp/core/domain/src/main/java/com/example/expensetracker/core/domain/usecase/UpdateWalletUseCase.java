package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;

import javax.inject.Inject;

public class UpdateWalletUseCase {
    private final IWalletRepository repository;

    @Inject
    public UpdateWalletUseCase(IWalletRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(WalletModel wallet) {
        try {
            repository.updateWallet(wallet);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

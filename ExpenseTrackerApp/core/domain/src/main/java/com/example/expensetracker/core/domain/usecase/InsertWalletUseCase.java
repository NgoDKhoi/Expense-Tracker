package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;

public class InsertWalletUseCase {
    private final IWalletRepository repository;

    @Inject
    public InsertWalletUseCase(IWalletRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(WalletModel wallet) {
        try {
            repository.insertWallet(wallet);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


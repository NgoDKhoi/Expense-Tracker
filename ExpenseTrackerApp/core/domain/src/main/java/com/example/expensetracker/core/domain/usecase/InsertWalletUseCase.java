package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;

public class InsertWalletUseCase {
    private final IWalletRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public InsertWalletUseCase(IWalletRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(WalletModel wallet) {
        try {
            repository.insertWallet(wallet);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;

import javax.inject.Inject;

public class UpdateWalletUseCase {
    private final IWalletRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public UpdateWalletUseCase(IWalletRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(WalletModel wallet) {
        try {
            repository.updateWallet(wallet);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

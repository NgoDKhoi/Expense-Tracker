package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;
import java.util.List;

public class GetAllWalletsUseCase {
    private final IWalletRepository repository;

    @Inject
    public GetAllWalletsUseCase(IWalletRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<WalletModel>> execute() {
        return repository.getAllWallets();
    }
}


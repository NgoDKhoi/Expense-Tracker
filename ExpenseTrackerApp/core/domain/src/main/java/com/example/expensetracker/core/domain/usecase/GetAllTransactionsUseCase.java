package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;
import java.util.List;

public class GetAllTransactionsUseCase {
    private final ITransactionRepository repository;

    @Inject
    public GetAllTransactionsUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<TransactionModel>> execute() {
        return repository.getAllExpenses();
    }
}


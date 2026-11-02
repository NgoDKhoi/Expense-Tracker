package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;

/**
 * Use case for adding an expense. Wallet balances are computed from transactions, so nothing else to update.
 */
public class AddTransactionUseCase {
    private final ITransactionRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public AddTransactionUseCase(ITransactionRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(TransactionModel expense) {
        try {
            repository.insertExpense(expense);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

import javax.inject.Inject;

/**
 * Use case for updating an existing transaction. Wallet balances recompute dynamically via Room queries.
 */
public class UpdateTransactionUseCase {
    private final ITransactionRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public UpdateTransactionUseCase(ITransactionRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(TransactionModel expense) {
        try {
            repository.updateExpense(expense);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

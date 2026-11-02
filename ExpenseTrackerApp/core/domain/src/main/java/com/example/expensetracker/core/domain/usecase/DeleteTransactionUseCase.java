package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;

public class DeleteTransactionUseCase {
    private final ITransactionRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public DeleteTransactionUseCase(ITransactionRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(TransactionModel expense) {
        try {
            repository.deleteExpense(expense);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }

    /** Undo a delete: the row was only soft-deleted, so bring it back instead of inserting a copy. */
    public Result<Void> undo(TransactionModel expense) {
        try {
            repository.restoreExpense(expense);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


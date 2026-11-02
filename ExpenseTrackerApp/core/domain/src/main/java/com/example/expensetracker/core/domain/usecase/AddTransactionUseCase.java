package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

/**
 * Use case for adding an expense. Wallet balances are computed from transactions, so nothing else to update.
 */
public class AddTransactionUseCase {
    private final ITransactionRepository repository;

    @Inject
    public AddTransactionUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(TransactionModel expense) {
        try {
            repository.insertExpense(expense);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


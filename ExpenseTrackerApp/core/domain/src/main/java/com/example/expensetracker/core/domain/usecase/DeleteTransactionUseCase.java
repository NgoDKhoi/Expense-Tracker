package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

public class DeleteTransactionUseCase {
    private final ITransactionRepository repository;

    @Inject
    public DeleteTransactionUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(TransactionModel expense) {
        try {
            repository.deleteExpense(expense);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


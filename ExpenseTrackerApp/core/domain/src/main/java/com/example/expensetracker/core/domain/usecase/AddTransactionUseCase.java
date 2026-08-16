package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

/**
 * Use case for adding an expense and updating the associated wallet balance.
 * Encapsulates the business logic of determining amount change based on transaction type.
 */
public class AddTransactionUseCase {
    private final ITransactionRepository repository;

    @Inject
    public AddTransactionUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    /**
     * Executes the use case: inserts an expense record and updates the wallet balance.
     * For INCOME type, the wallet balance increases; for EXPENSE type, it decreases.
     */
    public Result<Void> execute(TransactionModel expense) {
        try {
            double amountChange = "INCOME".equals(expense.getType())
                    ? expense.getAmount()
                    : -expense.getAmount();
            repository.insertExpenseAndUpdateWallet(expense, amountChange);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}


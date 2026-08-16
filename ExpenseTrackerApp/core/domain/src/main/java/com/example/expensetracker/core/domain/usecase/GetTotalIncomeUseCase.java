package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

public class GetTotalIncomeUseCase {
    private final ITransactionRepository repository;

    @Inject
    public GetTotalIncomeUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    public LiveData<Double> execute(long start, long end) {
        return repository.getTotalIncome(start, end);
    }
}


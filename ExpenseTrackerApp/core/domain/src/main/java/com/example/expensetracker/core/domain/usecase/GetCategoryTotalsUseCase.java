package com.example.expensetracker.core.domain.usecase;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;
import java.util.List;

public class GetCategoryTotalsUseCase {
    private final ITransactionRepository repository;

    @Inject
    public GetCategoryTotalsUseCase(ITransactionRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<CategoryTotalModel>> execute(long start, long end) {
        return repository.getCategoryTotals(start, end);
    }
}


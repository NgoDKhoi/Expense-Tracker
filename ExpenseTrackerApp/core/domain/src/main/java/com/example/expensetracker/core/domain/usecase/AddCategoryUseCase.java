package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;
import javax.inject.Inject;

public class AddCategoryUseCase {
    private final ICategoryRepository repository;
    private final ISyncScheduler syncScheduler;

    @Inject
    public AddCategoryUseCase(ICategoryRepository repository, ISyncScheduler syncScheduler) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
    }

    public Result<Void> execute(CategoryModel category) {
        try {
            repository.insertCategory(category);
            syncScheduler.requestSync();
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

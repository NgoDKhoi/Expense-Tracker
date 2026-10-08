package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;
import javax.inject.Inject;

public class AddCategoryUseCase {
    private final ICategoryRepository repository;

    @Inject
    public AddCategoryUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    public Result<Void> execute(CategoryModel category) {
        try {
            repository.insertCategory(category);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(e.getMessage(), e);
        }
    }
}

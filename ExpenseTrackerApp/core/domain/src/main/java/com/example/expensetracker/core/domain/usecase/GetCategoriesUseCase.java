package com.example.expensetracker.core.domain.usecase;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;
import java.util.List;
import javax.inject.Inject;

public class GetCategoriesUseCase {
    private final ICategoryRepository repository;

    @Inject
    public GetCategoriesUseCase(ICategoryRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<CategoryModel>> execute(String type) {
        if (type == null || type.trim().isEmpty() || "ALL".equalsIgnoreCase(type)) {
            return repository.getAllCategories();
        }
        return repository.getCategoriesByType(type);
    }
}

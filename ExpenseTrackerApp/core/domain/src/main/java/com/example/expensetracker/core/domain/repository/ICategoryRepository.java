package com.example.expensetracker.core.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.CategoryModel;
import java.util.List;

public interface ICategoryRepository {
    LiveData<List<CategoryModel>> getAllCategories();
    LiveData<List<CategoryModel>> getCategoriesByType(String type);
    void insertCategory(CategoryModel category);
    void deleteCategory(long categoryId);
}

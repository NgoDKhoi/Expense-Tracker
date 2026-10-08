package com.example.expensetracker.core.database.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.expensetracker.core.database.dao.CategoryDao;
import com.example.expensetracker.core.database.mapper.EntityMapper;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;

public class CategoryRepositoryImpl implements ICategoryRepository {

    private static final String TAG = "CategoryRepoImpl";
    private final CategoryDao categoryDao;
    private final ExecutorService executorService;

    @Inject
    public CategoryRepositoryImpl(CategoryDao categoryDao, ExecutorService executorService) {
        this.categoryDao = categoryDao;
        this.executorService = executorService;
    }

    @Override
    public LiveData<List<CategoryModel>> getAllCategories() {
        return Transformations.map(categoryDao.getAllCategories(), EntityMapper::toCategoryModelList);
    }

    @Override
    public LiveData<List<CategoryModel>> getCategoriesByType(String type) {
        return Transformations.map(categoryDao.getCategoriesByType(type), EntityMapper::toCategoryModelList);
    }

    @Override
    public void insertCategory(CategoryModel category) {
        executorService.execute(() -> {
            try {
                categoryDao.insertCategory(EntityMapper.toEntity(category));
            } catch (Exception e) {
                Log.e(TAG, "Error inserting category", e);
            }
        });
    }

    @Override
    public void deleteCategory(long categoryId) {
        executorService.execute(() -> {
            try {
                categoryDao.deleteCategory(categoryId);
            } catch (Exception e) {
                Log.e(TAG, "Error deleting category", e);
            }
        });
    }
}

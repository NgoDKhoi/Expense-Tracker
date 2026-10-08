package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.expensetracker.core.database.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategories(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE type = :type OR type = 'ALL' ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getCategoriesByType(String type);

    @Query("DELETE FROM categories WHERE id = :id")
    void deleteCategory(long id);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();
}

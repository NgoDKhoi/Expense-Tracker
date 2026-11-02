package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expensetracker.core.database.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategory(CategoryEntity category);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertCategories(List<CategoryEntity> categories);

    @Query("SELECT * FROM categories WHERE deleted = 0 ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getAllCategories();

    @Query("SELECT * FROM categories WHERE deleted = 0 AND (type = :type OR type = 'ALL') ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getCategoriesByType(String type);

    @Query("UPDATE categories SET deleted = 1, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    void deleteCategory(long id, long now);

    @Query("SELECT COUNT(*) FROM categories")
    int getCategoryCount();

    @Update
    void updateCategory(CategoryEntity category);

    // --- sync ---

    @Query("SELECT * FROM categories WHERE syncStatus = 'PENDING'")
    List<CategoryEntity> getPending();

    @Query("UPDATE categories SET syncStatus = 'SYNCED' WHERE uuid = :uuid AND updatedAt = :updatedAt")
    void markSynced(String uuid, long updatedAt);

    @Query("SELECT * FROM categories WHERE uuid = :uuid")
    CategoryEntity findByUuid(String uuid);

    @Query("DELETE FROM categories WHERE deleted = 1 AND syncStatus = 'SYNCED'")
    void purgeSyncedDeletes();
}

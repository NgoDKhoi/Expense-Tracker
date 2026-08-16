package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.expensetracker.core.database.entity.CategoryTotal;
import com.example.expensetracker.core.database.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insertExpense(TransactionEntity expense);

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getAllExpenses();

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<TransactionEntity>> getRecentExpenses(int limit);

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    List<TransactionEntity> getPendingExpenses();

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :start AND :end")
    LiveData<Double> getTotalExpense(long start, long end);

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp BETWEEN :start AND :end")
    LiveData<Double> getTotalIncome(long start, long end);

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    void updateSyncStatus(long id, String status);

    @Delete
    void deleteExpense(TransactionEntity expense);

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :start AND :end GROUP BY category")
    LiveData<List<CategoryTotal>> getCategoryTotals(long start, long end);
}

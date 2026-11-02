package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.expensetracker.core.database.entity.CategoryTotal;
import com.example.expensetracker.core.database.entity.PendingTransaction;
import com.example.expensetracker.core.database.entity.TransactionEntity;

import java.util.List;

@Dao
public interface TransactionDao {
    @Insert
    void insertExpense(TransactionEntity expense);

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY timestamp DESC")
    LiveData<List<TransactionEntity>> getAllExpenses();

    @Query("SELECT * FROM transactions WHERE deleted = 0 ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<TransactionEntity>> getRecentExpenses(int limit);

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING'")
    List<TransactionEntity> getPendingExpenses();

    @Query("SELECT SUM(amount) FROM transactions WHERE deleted = 0 AND type = 'EXPENSE' AND timestamp BETWEEN :start AND :end")
    LiveData<Double> getTotalExpense(long start, long end);

    @Query("SELECT SUM(amount) FROM transactions WHERE deleted = 0 AND type = 'INCOME' AND timestamp BETWEEN :start AND :end")
    LiveData<Double> getTotalIncome(long start, long end);

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    void updateSyncStatus(long id, String status);

    /** Soft delete (deleted = true) or undo (deleted = false); the row is kept so the change can sync. */
    @Query("UPDATE transactions SET deleted = :deleted, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    void setDeleted(long id, boolean deleted, long now);

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE deleted = 0 AND type = 'EXPENSE' AND timestamp BETWEEN :start AND :end GROUP BY category")
    LiveData<List<CategoryTotal>> getCategoryTotals(long start, long end);

    @Update
    void updateExpense(TransactionEntity expense);

    // --- sync ---

    // Transactions whose wallet row is gone (hard-deleted before v9) have no wallet uuid and stay local.
    @Query("SELECT t.*, w.uuid AS walletUuid FROM transactions t JOIN wallet_table w ON w.id = t.walletId WHERE t.syncStatus = 'PENDING'")
    List<PendingTransaction> getPendingWithWalletUuid();

    @Query("UPDATE transactions SET syncStatus = 'SYNCED' WHERE uuid = :uuid AND updatedAt = :updatedAt")
    void markSynced(String uuid, long updatedAt);

    @Query("SELECT * FROM transactions WHERE uuid = :uuid")
    TransactionEntity findByUuid(String uuid);

    @Query("DELETE FROM transactions WHERE deleted = 1 AND syncStatus = 'SYNCED'")
    void purgeSyncedDeletes();
}

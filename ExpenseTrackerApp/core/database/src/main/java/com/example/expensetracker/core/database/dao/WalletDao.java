package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.example.expensetracker.core.database.entity.WalletEntity;
import com.example.expensetracker.core.database.entity.WalletWithBalance;

import java.util.List;

@Dao
public interface WalletDao {
    String WITH_BALANCE = "SELECT w.*, w.initialBalance + COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)"
            + " FROM transactions t WHERE t.walletId = w.id AND t.deleted = 0), 0) AS balance FROM wallet_table w";

    @Insert
    void insertWallet(WalletEntity wallet);

    @Update
    void updateWallet(WalletEntity wallet);

    @Query(WITH_BALANCE + " WHERE w.deleted = 0 ORDER BY w.id")
    LiveData<List<WalletWithBalance>> getAllWallets();

    @Query(WITH_BALANCE + " WHERE w.id = :id")
    WalletWithBalance getWalletById(long id);

    @Query("UPDATE wallet_table SET deleted = 1, updatedAt = :now, syncStatus = 'PENDING' WHERE id = :id")
    void softDeleteWallet(long id, long now);

    @Query("UPDATE transactions SET deleted = 1, updatedAt = :now, syncStatus = 'PENDING' WHERE walletId = :walletId AND deleted = 0")
    void softDeleteTransactionsOfWallet(long walletId, long now);

    /** Deletes the wallet together with its transactions, so totals stay consistent. */
    @Transaction
    default void deleteWallet(long id, long now) {
        softDeleteWallet(id, now);
        softDeleteTransactionsOfWallet(id, now);
    }
}

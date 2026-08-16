package com.example.expensetracker.core.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.expensetracker.core.database.entity.WalletEntity;

import java.util.List;

@Dao
public interface WalletDao {
    @Insert
    void insertWallet(WalletEntity wallet);

    @Query("SELECT * FROM wallet_table")
    LiveData<List<WalletEntity>> getAllWallets();

    @Query("SELECT * FROM wallet_table WHERE id = :id")
    WalletEntity getWalletById(long id);

    @Query("UPDATE wallet_table SET balance = balance + :amountChange WHERE id = :walletId")
    void updateBalance(long walletId, double amountChange);
}

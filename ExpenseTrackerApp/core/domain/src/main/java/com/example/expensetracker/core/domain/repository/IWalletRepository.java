package com.example.expensetracker.core.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.WalletModel;
import java.util.List;

public interface IWalletRepository {
    LiveData<List<WalletModel>> getAllWallets();
    void insertWallet(WalletModel wallet);
    void updateWallet(WalletModel wallet);
    void deleteWallet(long id);
    WalletModel getWalletById(long id);
    void updateBalance(long walletId, double amountChange);
}

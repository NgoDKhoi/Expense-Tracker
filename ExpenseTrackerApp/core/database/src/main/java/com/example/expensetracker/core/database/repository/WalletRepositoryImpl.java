package com.example.expensetracker.core.database.repository;
import javax.inject.Inject;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.mapper.EntityMapper;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IWalletRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class WalletRepositoryImpl implements IWalletRepository {
    private static final String TAG = "WalletRepositoryImpl";
    private final WalletDao walletDao;
    private final ExecutorService executorService;

    @Inject
    public WalletRepositoryImpl(WalletDao walletDao, ExecutorService executorService) {
        this.walletDao = walletDao;
        this.executorService = executorService;
    }

    @Override
    public void insertWallet(WalletModel wallet) {
        executorService.execute(() -> {
            try {
                walletDao.insertWallet(EntityMapper.toEntity(wallet));
            } catch (Exception e) {
                Log.d(TAG, "Error inserting wallet", e);
            }
        });
    }

    @Override
    public LiveData<List<WalletModel>> getAllWallets() {
        return Transformations.map(walletDao.getAllWallets(), EntityMapper::toWalletModelList);
    }
    
    @Override
    public WalletModel getWalletById(long id) {
        try {
            return EntityMapper.toModel(walletDao.getWalletById(id));
        } catch (Exception e) {
            Log.d(TAG, "Error getting wallet by id", e);
            return null;
        }
    }

    @Override
    public void updateBalance(long walletId, double amountChange) {
        executorService.execute(() -> {
            try {
                walletDao.updateBalance(walletId, amountChange);
            } catch (Exception e) {
                Log.d(TAG, "Error updating balance", e);
            }
        });
    }
}



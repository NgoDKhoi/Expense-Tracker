package com.example.expensetracker.core.database.repository;

import javax.inject.Inject;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.mapper.EntityMapper;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class TransactionRepositoryImpl implements ITransactionRepository {
    private static final String TAG = "TransactionRepositoryImpl";
    private final TransactionDao transactionDao;
    private final WalletDao walletDao;
    private final ExecutorService executorService;

    @Inject
    public TransactionRepositoryImpl(TransactionDao transactionDao, WalletDao walletDao, ExecutorService executorService) {
        this.transactionDao = transactionDao;
        this.walletDao = walletDao;
        this.executorService = executorService;
    }

    @Override
    public void insertExpense(TransactionModel expense) {
        executorService.execute(() -> {
            try {
                transactionDao.insertExpense(EntityMapper.toEntity(expense));
            } catch (Exception e) {
                Log.d(TAG, "Error inserting expense", e);
            }
        });
    }

    @Override
    public LiveData<List<TransactionModel>> getAllExpenses() {
        return Transformations.map(transactionDao.getAllExpenses(), EntityMapper::toModelList);
    }

    @Override
    public LiveData<List<TransactionModel>> getRecentExpenses(int limit) {
        return Transformations.map(transactionDao.getRecentExpenses(limit), EntityMapper::toModelList);
    }

    @Override
    public List<TransactionModel> getPendingExpenses() {
        try {
            return EntityMapper.toModelList(transactionDao.getPendingExpenses());
        } catch (Exception e) {
            Log.d(TAG, "Error getting pending expenses", e);
            return null;
        }
    }

    @Override
    public LiveData<Double> getTotalExpense(long start, long end) {
        return transactionDao.getTotalExpense(start, end);
    }

    @Override
    public LiveData<Double> getTotalIncome(long start, long end) {
        return transactionDao.getTotalIncome(start, end);
    }

    @Override
    public void updateSyncStatus(long id, String status) {
        executorService.execute(() -> {
            try {
                transactionDao.updateSyncStatus(id, status);
            } catch (Exception e) {
                Log.d(TAG, "Error updating sync status", e);
            }
        });
    }

    @Override
    public void deleteExpense(TransactionModel expense) {
        executorService.execute(() -> {
            try {
                transactionDao.deleteExpense(EntityMapper.toEntity(expense));
            } catch (Exception e) {
                Log.d(TAG, "Error deleting expense", e);
            }
        });
    }

    @Override
    public LiveData<List<CategoryTotalModel>> getCategoryTotals(long start, long end) {
        return Transformations.map(transactionDao.getCategoryTotals(start, end), EntityMapper::toCategoryTotalModelList);
    }

    @Override
    public void insertExpenseAndUpdateWallet(TransactionModel expense, double amountChange) {
        executorService.execute(() -> {
            try {
                transactionDao.insertExpense(EntityMapper.toEntity(expense));
                walletDao.updateBalance(expense.getWalletId(), amountChange);
            } catch (Exception e) {
                Log.d(TAG, "Error inserting expense and updating wallet", e);
            }
        });
    }
}


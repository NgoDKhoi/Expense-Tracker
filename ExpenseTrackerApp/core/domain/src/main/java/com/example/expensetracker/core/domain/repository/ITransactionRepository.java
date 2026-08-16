package com.example.expensetracker.core.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import java.util.List;

public interface ITransactionRepository {
    void insertExpense(TransactionModel expense);
    void insertExpenseAndUpdateWallet(TransactionModel expense, double amountChange);
    LiveData<List<TransactionModel>> getAllExpenses();
    LiveData<List<TransactionModel>> getRecentExpenses(int limit);
    List<TransactionModel> getPendingExpenses();
    LiveData<Double> getTotalExpense(long start, long end);
    LiveData<Double> getTotalIncome(long start, long end);
    void updateSyncStatus(long id, String status);
    LiveData<List<CategoryTotalModel>> getCategoryTotals(long start, long end);
    void deleteExpense(TransactionModel expense);
}

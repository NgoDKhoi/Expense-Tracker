package com.example.expensetracker.viewmodel;

import javax.inject.Inject;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteWalletUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoryTotalsUseCase;
import com.example.expensetracker.core.domain.usecase.GetRecentTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalExpenseUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalIncomeUseCase;
import com.example.expensetracker.core.domain.usecase.InsertWalletUseCase;
import com.example.expensetracker.core.domain.usecase.UpdateWalletUseCase;

import java.util.Calendar;
import java.util.List;

/**
 * ViewModel for the Home/Dashboard screen.
 * Receives all dependencies via constructor injection (no Android Context).
 */
public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    public enum TimeFilter { DAY, MONTH, YEAR }

    private final GetRecentTransactionsUseCase getRecentTransactionsUseCase;
    private final GetAllTransactionsUseCase getAllTransactionsUseCase;
    private final GetTotalExpenseUseCase getTotalExpenseUseCase;
    private final GetTotalIncomeUseCase getTotalIncomeUseCase;
    private final GetCategoryTotalsUseCase getCategoryTotalsUseCase;
    private final GetAllWalletsUseCase getAllWalletsUseCase;
    private final InsertWalletUseCase insertWalletUseCase;
    private final UpdateWalletUseCase updateWalletUseCase;
    private final DeleteWalletUseCase deleteWalletUseCase;
    private final DeleteTransactionUseCase deleteTransactionUseCase;
    private final AddTransactionUseCase addTransactionUseCase;

    private final LiveData<List<TransactionModel>> recentExpenses;
    private final LiveData<List<WalletModel>> wallets;

    private final MutableLiveData<Pair<Long, Long>> timeRange = new MutableLiveData<>();
    private final LiveData<Double> totalExpense;
    private final LiveData<Double> totalIncome;
    private final LiveData<List<CategoryTotalModel>> categoryTotals;

    @Inject
    public HomeViewModel(
            GetRecentTransactionsUseCase getRecentTransactionsUseCase,
            GetAllTransactionsUseCase getAllTransactionsUseCase,
            GetTotalExpenseUseCase getTotalExpenseUseCase,
            GetTotalIncomeUseCase getTotalIncomeUseCase,
            GetCategoryTotalsUseCase getCategoryTotalsUseCase,
            GetAllWalletsUseCase getAllWalletsUseCase,
            InsertWalletUseCase insertWalletUseCase,
            UpdateWalletUseCase updateWalletUseCase,
            DeleteWalletUseCase deleteWalletUseCase,
            DeleteTransactionUseCase deleteTransactionUseCase,
            AddTransactionUseCase addTransactionUseCase) {

        Log.d(TAG, "HomeViewModel created with injected use cases.");

        this.getRecentTransactionsUseCase = getRecentTransactionsUseCase;
        this.getAllTransactionsUseCase = getAllTransactionsUseCase;
        this.getTotalExpenseUseCase = getTotalExpenseUseCase;
        this.getTotalIncomeUseCase = getTotalIncomeUseCase;
        this.getCategoryTotalsUseCase = getCategoryTotalsUseCase;
        this.getAllWalletsUseCase = getAllWalletsUseCase;
        this.insertWalletUseCase = insertWalletUseCase;
        this.updateWalletUseCase = updateWalletUseCase;
        this.deleteWalletUseCase = deleteWalletUseCase;
        this.deleteTransactionUseCase = deleteTransactionUseCase;
        this.addTransactionUseCase = addTransactionUseCase;

        recentExpenses = getRecentTransactionsUseCase.execute(5);
        wallets = getAllWalletsUseCase.execute();

        totalExpense = Transformations.switchMap(timeRange, range ->
                getTotalExpenseUseCase.execute(range.first, range.second)
        );

        totalIncome = Transformations.switchMap(timeRange, range ->
                getTotalIncomeUseCase.execute(range.first, range.second)
        );

        categoryTotals = Transformations.switchMap(timeRange, range ->
                getCategoryTotalsUseCase.execute(range.first, range.second)
        );

        setTimeFilter(TimeFilter.MONTH);
    }

    public void setTimeFilter(TimeFilter filter) {
        Log.d(TAG, "setTimeFilter: " + filter.name());
        Calendar calendar = Calendar.getInstance();
        long start, end;

        switch (filter) {
            case DAY:
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                start = calendar.getTimeInMillis();

                calendar.set(Calendar.HOUR_OF_DAY, 23);
                calendar.set(Calendar.MINUTE, 59);
                calendar.set(Calendar.SECOND, 59);
                calendar.set(Calendar.MILLISECOND, 999);
                end = calendar.getTimeInMillis();
                break;
            case MONTH:
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                start = calendar.getTimeInMillis();

                calendar.add(Calendar.MONTH, 1);
                calendar.add(Calendar.MILLISECOND, -1);
                end = calendar.getTimeInMillis();
                break;
            case YEAR:
            default:
                calendar.set(Calendar.DAY_OF_YEAR, 1);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                start = calendar.getTimeInMillis();

                calendar.add(Calendar.YEAR, 1);
                calendar.add(Calendar.MILLISECOND, -1);
                end = calendar.getTimeInMillis();
                break;
        }
        timeRange.setValue(new Pair<>(start, end));
    }

    public LiveData<List<TransactionModel>> getRecentExpenses() {
        return recentExpenses;
    }

    public LiveData<Double> getTotalExpense() {
        return totalExpense;
    }

    public LiveData<Double> getTotalIncome() {
        return totalIncome;
    }

    public LiveData<List<TransactionModel>> getAllExpenses() {
        return getAllTransactionsUseCase.execute();
    }

    public LiveData<List<WalletModel>> getWallets() {
        return wallets;
    }

    public LiveData<List<CategoryTotalModel>> getCategoryTotals() {
        return categoryTotals;
    }

    public void insertWallet(String name, double initialBalance) {
        insertWallet(name, initialBalance, "#FF5722", "account_balance_wallet");
    }

    public void insertWallet(String name, double initialBalance, String colorHex, String iconName) {
        Log.d(TAG, "insertWallet: name=" + name + ", balance=" + initialBalance);
        WalletModel wallet = new WalletModel();
        wallet.setName(name);
        wallet.setBalance(initialBalance);
        wallet.setColorHex(colorHex);
        wallet.setIconName(iconName);
        insertWalletUseCase.execute(wallet);
    }

    public void updateWallet(WalletModel wallet) {
        updateWalletUseCase.execute(wallet);
    }

    public void deleteWallet(long walletId) {
        deleteWalletUseCase.execute(walletId);
    }

    public void deleteTransaction(TransactionModel transaction) {
        deleteTransactionUseCase.execute(transaction);
    }

    public void undoDeleteTransaction(TransactionModel transaction) {
        addTransactionUseCase.execute(transaction);
    }
}

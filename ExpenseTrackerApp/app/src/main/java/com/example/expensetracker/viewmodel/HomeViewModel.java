package com.example.expensetracker.viewmodel;

import javax.inject.Inject;

import android.util.Log;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteWalletUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoryTotalsUseCase;
import com.example.expensetracker.core.domain.usecase.GetRecentTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalExpenseUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalIncomeUseCase;
import com.example.expensetracker.core.domain.usecase.InsertWalletUseCase;
import com.example.expensetracker.core.domain.usecase.UpdateWalletUseCase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * ViewModel for the Home/Dashboard screen.
 * Receives all dependencies via constructor injection (no Android Context).
 */
public class HomeViewModel extends ViewModel {

    private static final String TAG = "HomeViewModel";

    public enum TimeFilter { DAY, MONTH, YEAR }

    public static final class TrendBucket {
        public final String label;
        public final double income;
        public final double expense;

        public TrendBucket(String label, double income, double expense) {
            this.label = label;
            this.income = income;
            this.expense = expense;
        }
    }

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
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final AddCategoryUseCase addCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    private final LiveData<List<TransactionModel>> recentExpenses;
    private final LiveData<List<TransactionModel>> allTransactions;
    private final LiveData<List<WalletModel>> wallets;
    private final LiveData<List<CategoryModel>> allCategories;

    private final MutableLiveData<Pair<Long, Long>> timeRange = new MutableLiveData<>();
    private TimeFilter currentTimeFilter = TimeFilter.MONTH;
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
            GetCategoriesUseCase getCategoriesUseCase,
            AddCategoryUseCase addCategoryUseCase,
            DeleteCategoryUseCase deleteCategoryUseCase) {

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
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.addCategoryUseCase = addCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;

        recentExpenses = getRecentTransactionsUseCase.execute(5);
        allTransactions = getAllTransactionsUseCase.execute();
        wallets = getAllWalletsUseCase.execute();
        allCategories = getCategoriesUseCase.execute("ALL");

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
        this.currentTimeFilter = filter != null ? filter : TimeFilter.MONTH;
        Log.d(TAG, "setTimeFilter: " + currentTimeFilter.name());
        Calendar calendar = Calendar.getInstance();
        long start, end;

        switch (currentTimeFilter) {
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

    public TimeFilter getCurrentTimeFilter() {
        return currentTimeFilter;
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
        return allTransactions;
    }

    public LiveData<List<WalletModel>> getWallets() {
        return wallets;
    }

    public LiveData<List<CategoryTotalModel>> getCategoryTotals() {
        return categoryTotals;
    }

    public LiveData<List<CategoryModel>> getAllCategories() {
        return allCategories;
    }

    public void addCategory(String name, String type, String colorHex, String iconName) {
        addCategoryUseCase.execute(new CategoryModel(name, type, colorHex, iconName));
    }

    public void deleteCategory(long id) {
        deleteCategoryUseCase.execute(id);
    }

    /**
     * Groups transactions into chronological Income/Expense buckets for the BarChart trend view.
     */
    public static List<TrendBucket> buildTrendBuckets(List<TransactionModel> allTransactions,
                                                      TimeFilter filter,
                                                      long nowMillis) {
        TimeFilter effectiveFilter = filter != null ? filter : TimeFilter.MONTH;
        List<TrendBucket> result = new ArrayList<>();

        if (effectiveFilter == TimeFilter.DAY) {
            final int count = 7;
            long[] starts = new long[count];
            long[] ends = new long[count];
            String[] labels = new String[count];
            double[] incomes = new double[count];
            double[] expenses = new double[count];
            SimpleDateFormat dayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());

            for (int i = 0; i < count; i++) {
                int offset = i - (count - 1);
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(nowMillis);
                cal.add(Calendar.DAY_OF_YEAR, offset);
                labels[i] = dayFormat.format(cal.getTime());

                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                starts[i] = cal.getTimeInMillis();

                cal.set(Calendar.HOUR_OF_DAY, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);
                cal.set(Calendar.MILLISECOND, 999);
                ends[i] = cal.getTimeInMillis();
            }

            if (allTransactions != null) {
                for (TransactionModel tx : allTransactions) {
                    if (tx == null || tx.isDeleted() || tx.getTimestamp() == null) continue;
                    long ts = tx.getTimestamp().getTime();
                    for (int i = 0; i < count; i++) {
                        if (ts >= starts[i] && ts <= ends[i]) {
                            if ("INCOME".equalsIgnoreCase(tx.getType())) {
                                incomes[i] += tx.getAmount();
                            } else {
                                expenses[i] += tx.getAmount();
                            }
                            break;
                        }
                    }
                }
            }

            for (int i = 0; i < count; i++) {
                result.add(new TrendBucket(labels[i], incomes[i], expenses[i]));
            }
            return result;
        }

        Calendar refCal = Calendar.getInstance();
        refCal.setTimeInMillis(nowMillis);
        int targetYear = refCal.get(Calendar.YEAR);
        int targetMonth = refCal.get(Calendar.MONTH);

        if (effectiveFilter == TimeFilter.MONTH) {
            final int count = 4;
            double[] incomes = new double[count];
            double[] expenses = new double[count];

            if (allTransactions != null) {
                Calendar txCal = Calendar.getInstance();
                for (TransactionModel tx : allTransactions) {
                    if (tx == null || tx.isDeleted() || tx.getTimestamp() == null) continue;
                    txCal.setTimeInMillis(tx.getTimestamp().getTime());
                    if (txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.MONTH) == targetMonth) {
                        int weekIdx = Math.min(count - 1, (txCal.get(Calendar.DAY_OF_MONTH) - 1) / 7);
                        if ("INCOME".equalsIgnoreCase(tx.getType())) {
                            incomes[weekIdx] += tx.getAmount();
                        } else {
                            expenses[weekIdx] += tx.getAmount();
                        }
                    }
                }
            }

            for (int i = 0; i < count; i++) {
                result.add(new TrendBucket("T" + (i + 1), incomes[i], expenses[i]));
            }
            return result;
        }

        // YEAR: 12 months T1..T12
        final int count = 12;
        double[] incomes = new double[count];
        double[] expenses = new double[count];

        if (allTransactions != null) {
            Calendar txCal = Calendar.getInstance();
            for (TransactionModel tx : allTransactions) {
                if (tx == null || tx.isDeleted() || tx.getTimestamp() == null) continue;
                txCal.setTimeInMillis(tx.getTimestamp().getTime());
                if (txCal.get(Calendar.YEAR) == targetYear) {
                    int monthIdx = txCal.get(Calendar.MONTH);
                    if (monthIdx >= 0 && monthIdx < count) {
                        if ("INCOME".equalsIgnoreCase(tx.getType())) {
                            incomes[monthIdx] += tx.getAmount();
                        } else {
                            expenses[monthIdx] += tx.getAmount();
                        }
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            result.add(new TrendBucket("T" + (i + 1), incomes[i], expenses[i]));
        }
        return result;
    }

    public void insertWallet(String name, double initialBalance) {
        insertWallet(name, initialBalance, "#FF5722", "account_balance_wallet");
    }

    public void insertWallet(String name, double initialBalance, String colorHex, String iconName) {
        Log.d(TAG, "insertWallet: name=" + name + ", balance=" + initialBalance);
        WalletModel wallet = new WalletModel();
        wallet.setName(name);
        wallet.setInitialBalance(initialBalance);
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
        deleteTransactionUseCase.undo(transaction);
    }
}

package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.model.CategoryModel;
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
import com.example.expensetracker.util.CsvExportHelper;
import com.example.expensetracker.util.CurrencyFormatter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private GetAllTransactionsUseCase getAllTransactionsUseCase;
    @Mock private GetRecentTransactionsUseCase getRecentTransactionsUseCase;
    @Mock private GetTotalExpenseUseCase getTotalExpenseUseCase;
    @Mock private GetTotalIncomeUseCase getTotalIncomeUseCase;
    @Mock private GetCategoryTotalsUseCase getCategoryTotalsUseCase;
    @Mock private GetAllWalletsUseCase getAllWalletsUseCase;
    @Mock private InsertWalletUseCase insertWalletUseCase;
    @Mock private UpdateWalletUseCase updateWalletUseCase;
    @Mock private DeleteWalletUseCase deleteWalletUseCase;
    @Mock private DeleteTransactionUseCase deleteTransactionUseCase;
    @Mock private GetCategoriesUseCase getCategoriesUseCase;
    @Mock private AddCategoryUseCase addCategoryUseCase;
    @Mock private DeleteCategoryUseCase deleteCategoryUseCase;

    private HomeViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        when(getAllTransactionsUseCase.execute()).thenReturn(new MutableLiveData<>());
        when(getRecentTransactionsUseCase.execute(anyInt())).thenReturn(new MutableLiveData<>());
        when(getTotalExpenseUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getTotalIncomeUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getCategoryTotalsUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getAllWalletsUseCase.execute()).thenReturn(new MutableLiveData<>());
        when(getCategoriesUseCase.execute("ALL")).thenReturn(new MutableLiveData<>());

        viewModel = new HomeViewModel(
                getRecentTransactionsUseCase,
                getAllTransactionsUseCase,
                getTotalExpenseUseCase,
                getTotalIncomeUseCase,
                getCategoryTotalsUseCase,
                getAllWalletsUseCase,
                insertWalletUseCase,
                updateWalletUseCase,
                deleteWalletUseCase,
                deleteTransactionUseCase,
                getCategoriesUseCase,
                addCategoryUseCase,
                deleteCategoryUseCase
        );
    }

    @Test
    public void testGetRecentExpenses_ReturnsExpectedLiveData() {
        var result = viewModel.getRecentExpenses();
        assertNotNull(result);
    }

    @Test
    public void testInsertWallet_CallsUseCase() {
        viewModel.insertWallet("Main Wallet", 500.0);
        verify(insertWalletUseCase).execute(any(WalletModel.class));
    }

    @Test
    public void testDeleteTransaction_CallsUseCase() {
        TransactionModel tx = new TransactionModel();
        viewModel.deleteTransaction(tx);
        verify(deleteTransactionUseCase).execute(tx);
    }

    @Test
    public void testUndoDeleteTransaction_CallsUseCase() {
        TransactionModel tx = new TransactionModel();
        viewModel.undoDeleteTransaction(tx);
        verify(deleteTransactionUseCase).undo(tx);
    }

    @Test
    public void buildTrendBuckets_day_returns7DaysAndIgnoresDeletedOrOutOfRange() {
        long now = timestampOf(2026, Calendar.NOVEMBER, 17, 14, 0);
        long twoDaysAgo = timestampOf(2026, Calendar.NOVEMBER, 15, 9, 30);
        long tenDaysAgo = timestampOf(2026, Calendar.NOVEMBER, 7, 10, 0);

        TransactionModel incomeToday = tx(1L, 500_000, "INCOME", "Bonus", now, false);
        TransactionModel expenseTwoDaysAgo = tx(2L, 80_000, "EXPENSE", "Food", twoDaysAgo, false);
        TransactionModel deletedToday = tx(3L, 999_000, "EXPENSE", "Other", now, true);
        TransactionModel oldExpense = tx(4L, 200_000, "EXPENSE", "Shopping", tenDaysAgo, false);

        List<HomeViewModel.TrendBucket> buckets = HomeViewModel.buildTrendBuckets(
                Arrays.asList(incomeToday, expenseTwoDaysAgo, deletedToday, oldExpense),
                HomeViewModel.TimeFilter.DAY,
                now
        );

        assertEquals(7, buckets.size());
        assertEquals(80_000.0, buckets.get(4).expense, 0.01);
        assertEquals(0.0, buckets.get(4).income, 0.01);
        assertEquals(500_000.0, buckets.get(6).income, 0.01);
        assertEquals(0.0, buckets.get(6).expense, 0.01);
    }

    @Test
    public void buildTrendBuckets_month_returns4WeeksWithCorrectGrouping() {
        long now = timestampOf(2026, Calendar.NOVEMBER, 25, 12, 0);
        TransactionModel w1 = tx(1L, 100_000, "EXPENSE", "Food", timestampOf(2026, Calendar.NOVEMBER, 3, 10, 0), false);
        TransactionModel w2 = tx(2L, 200_000, "INCOME", "Salary", timestampOf(2026, Calendar.NOVEMBER, 10, 10, 0), false);
        TransactionModel w3 = tx(3L, 50_000, "EXPENSE", "Transport", timestampOf(2026, Calendar.NOVEMBER, 18, 10, 0), false);
        TransactionModel w4 = tx(4L, 300_000, "EXPENSE", "Shopping", timestampOf(2026, Calendar.NOVEMBER, 29, 10, 0), false);
        TransactionModel otherMonth = tx(5L, 900_000, "EXPENSE", "Other", timestampOf(2026, Calendar.OCTOBER, 29, 10, 0), false);

        List<HomeViewModel.TrendBucket> buckets = HomeViewModel.buildTrendBuckets(
                Arrays.asList(w1, w2, w3, w4, otherMonth),
                HomeViewModel.TimeFilter.MONTH,
                now
        );

        assertEquals(4, buckets.size());
        assertEquals("T1", buckets.get(0).label);
        assertEquals(100_000.0, buckets.get(0).expense, 0.01);
        assertEquals("T2", buckets.get(1).label);
        assertEquals(200_000.0, buckets.get(1).income, 0.01);
        assertEquals("T3", buckets.get(2).label);
        assertEquals(50_000.0, buckets.get(2).expense, 0.01);
        assertEquals("T4", buckets.get(3).label);
        assertEquals(300_000.0, buckets.get(3).expense, 0.01);
    }

    @Test
    public void buildTrendBuckets_year_returns12Months() {
        long now = timestampOf(2026, Calendar.NOVEMBER, 17, 12, 0);
        TransactionModel jan = tx(1L, 1_000_000, "INCOME", "Salary", timestampOf(2026, Calendar.JANUARY, 5, 8, 0), false);
        TransactionModel may = tx(2L, 250_000, "EXPENSE", "Utilities", timestampOf(2026, Calendar.MAY, 15, 8, 0), false);
        TransactionModel dec = tx(3L, 400_000, "EXPENSE", "Food", timestampOf(2026, Calendar.DECEMBER, 20, 8, 0), false);
        TransactionModel lastYear = tx(4L, 700_000, "EXPENSE", "Food", timestampOf(2025, Calendar.JANUARY, 5, 8, 0), false);

        List<HomeViewModel.TrendBucket> buckets = HomeViewModel.buildTrendBuckets(
                Arrays.asList(jan, may, dec, lastYear),
                HomeViewModel.TimeFilter.YEAR,
                now
        );

        assertEquals(12, buckets.size());
        assertEquals("T1", buckets.get(0).label);
        assertEquals(1_000_000.0, buckets.get(0).income, 0.01);
        assertEquals("T5", buckets.get(4).label);
        assertEquals(250_000.0, buckets.get(4).expense, 0.01);
        assertEquals("T12", buckets.get(11).label);
        assertEquals(400_000.0, buckets.get(11).expense, 0.01);
    }

    @Test
    public void csvExportHelper_buildCsvContent_includesBomHeadersAndEscapesSpecialChars() {
        long ts = timestampOf(2026, Calendar.NOVEMBER, 17, 8, 30);
        TransactionModel tx = new TransactionModel.Builder()
                .id(7L)
                .amount(45_000)
                .category("Food")
                .timestamp(new Date(ts))
                .note("Ăn phở, \"đặc biệt\"")
                .walletId(1L)
                .type("EXPENSE")
                .build();

        String csv = CsvExportHelper.buildCsvContent(
                Collections.singletonList(tx),
                Map.of(1L, "Tiền mặt")
        );

        assertTrue(csv.startsWith("\uFEFF"));
        String[] lines = csv.split("\n");
        assertEquals(2, lines.length);
        assertEquals("\uFEFFID,Ngày giờ,Loại,Danh mục,Số tiền,Ví,Ghi chú", lines[0]);
        assertEquals("7,2026-11-17 08:30,EXPENSE,Food,45000,Tiền mặt,\"Ăn phở, \"\"đặc biệt\"\"\"", lines[1]);
    }

    @Test
    public void deleteCategoryUseCase_isCustomCategory_protectsDefaultsAndDetectsCustom() {
        CategoryModel seededFood = new CategoryModel(1L, "Food", "EXPENSE", "#4CAF50", "restaurant");
        CategoryModel seededDrink = new CategoryModel(2L, "Drink", "EXPENSE", "#00BCD4", "local_cafe");
        CategoryModel seededSalary = new CategoryModel(7L, "Salary", "INCOME", "#4CAF50", "payments");
        CategoryModel userCustom = new CategoryModel(11L, "Thú cưng", "EXPENSE", "#FF9800", "custom");
        CategoryModel userNonDefaultName = new CategoryModel(12L, "Coffee", "EXPENSE", "#795548", "cafe");

        assertFalse(DeleteCategoryUseCase.isCustomCategory(null));
        assertFalse(DeleteCategoryUseCase.isCustomCategory(seededFood));
        assertFalse(DeleteCategoryUseCase.isCustomCategory(seededDrink));
        assertFalse(DeleteCategoryUseCase.isCustomCategory(seededSalary));
        assertTrue(DeleteCategoryUseCase.isCustomCategory(userCustom));
        assertTrue(DeleteCategoryUseCase.isCustomCategory(userNonDefaultName));
    }

    @Test
    public void currencyFormatter_formatsPositiveAndNegativeWithSelectedSymbol() {
        assertEquals("2.5M $", CurrencyFormatter.formatCompactWithSymbol(2_500_000, "$"));
        assertEquals("-1.5M €", CurrencyFormatter.formatCompactWithSymbol(-1_500_000, "€"));
        assertEquals("45,000 ₫", CurrencyFormatter.formatFullWithSymbol(45_000, "₫"));
    }

    private static long timestampOf(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private static TransactionModel tx(long id, double amount, String type, String category, long ts, boolean deleted) {
        TransactionModel model = new TransactionModel.Builder()
                .id(id)
                .amount(amount)
                .type(type)
                .category(category)
                .timestamp(new Date(ts))
                .walletId(1L)
                .build();
        model.setDeleted(deleted);
        return model;
    }
}

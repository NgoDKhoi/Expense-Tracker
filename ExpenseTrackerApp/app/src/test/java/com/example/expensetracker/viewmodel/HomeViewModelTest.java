package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertNotNull;
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
    @Mock private AddTransactionUseCase addTransactionUseCase;

    private HomeViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Mock default behaviors
        when(getAllTransactionsUseCase.execute()).thenReturn(new MutableLiveData<>());
        when(getRecentTransactionsUseCase.execute(anyInt())).thenReturn(new MutableLiveData<>());
        when(getTotalExpenseUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getTotalIncomeUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getCategoryTotalsUseCase.execute(anyLong(), anyLong())).thenReturn(new MutableLiveData<>());
        when(getAllWalletsUseCase.execute()).thenReturn(new MutableLiveData<>());

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
                addTransactionUseCase
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
        verify(addTransactionUseCase).execute(tx);
    }
}

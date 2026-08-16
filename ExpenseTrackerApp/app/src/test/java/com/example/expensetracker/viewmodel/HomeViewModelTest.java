package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.model.CategoryTotalModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.GetAllTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoryTotalsUseCase;
import com.example.expensetracker.core.domain.usecase.GetRecentTransactionsUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalExpenseUseCase;
import com.example.expensetracker.core.domain.usecase.GetTotalIncomeUseCase;
import com.example.expensetracker.core.domain.usecase.InsertWalletUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
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
                insertWalletUseCase
        );
    }

    @Test
    public void testGetRecentExpenses_ReturnsExpectedLiveData() {
        // Act
        var result = viewModel.getRecentExpenses();
        
        // Assert
        org.junit.Assert.assertNotNull(result);
    }

    @Test
    public void testInsertWallet_CallsUseCase() {
        // Act
        viewModel.insertWallet("Main Wallet", 500.0);
        
        // Assert
        verify(insertWalletUseCase).execute(any(WalletModel.class));
    }
}

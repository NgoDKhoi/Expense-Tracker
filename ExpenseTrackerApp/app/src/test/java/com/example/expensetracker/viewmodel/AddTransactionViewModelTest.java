package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddTransactionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private AddTransactionUseCase addTransactionUseCase;
    @Mock private GetAllWalletsUseCase getAllWalletsUseCase;
    @Mock private GetCategoriesUseCase getCategoriesUseCase;
    @Mock private AddCategoryUseCase addCategoryUseCase;

    private AddTransactionViewModel viewModel;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(getAllWalletsUseCase.execute()).thenReturn(new MutableLiveData<>(new ArrayList<>()));

        viewModel = new AddTransactionViewModel(
                addTransactionUseCase,
                getAllWalletsUseCase,
                getCategoriesUseCase,
                addCategoryUseCase
        );
    }

    @Test
    public void testGetWallets_NotNull() {
        assertNotNull(viewModel.getWallets());
    }

    @Test
    public void testGetCategories_CallsUseCase() {
        MutableLiveData<List<CategoryModel>> liveData = new MutableLiveData<>(new ArrayList<>());
        when(getCategoriesUseCase.execute("EXPENSE")).thenReturn(liveData);

        var result = viewModel.getCategories("EXPENSE");
        assertNotNull(result);
        verify(getCategoriesUseCase).execute("EXPENSE");
    }

    @Test
    public void testAddCategory_CallsUseCase() {
        viewModel.addCategory("Shopping", "EXPENSE", "#9C27B0", "custom");
        verify(addCategoryUseCase).execute(any(CategoryModel.class));
    }

    @Test
    public void testSaveExpense_CallsUseCaseWithExpectedParameters() {
        Date customDate = new Date(1700000000000L);
        viewModel.saveExpense(250.0, "Lunch", "Food", "uri://test", "Good food", 1L, "EXPENSE", customDate);

        ArgumentCaptor<TransactionModel> captor = ArgumentCaptor.forClass(TransactionModel.class);
        verify(addTransactionUseCase).execute(captor.capture());

        TransactionModel saved = captor.getValue();
        assertEquals(250.0, saved.getAmount(), 0.001);
        assertEquals("Lunch", saved.getTitle());
        assertEquals("Food", saved.getCategory());
        assertEquals("Good food", saved.getNote());
        assertEquals(1L, saved.getWalletId());
        assertEquals("EXPENSE", saved.getType());
        assertEquals(customDate, saved.getTimestamp());
    }
}

package com.example.expensetracker.viewmodel;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.DeleteCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;
import com.example.expensetracker.viewmodel.AddTransactionViewModel.ParsedVoiceExpense;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddTransactionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AddTransactionUseCase addTransactionUseCase;
    private GetAllWalletsUseCase getAllWalletsUseCase;
    private GetCategoriesUseCase getCategoriesUseCase;
    private AddCategoryUseCase addCategoryUseCase;
    private DeleteCategoryUseCase deleteCategoryUseCase;
    private AddTransactionViewModel viewModel;

    @Before
    public void setUp() {
        addTransactionUseCase = mock(AddTransactionUseCase.class);
        getAllWalletsUseCase = mock(GetAllWalletsUseCase.class);
        getCategoriesUseCase = mock(GetCategoriesUseCase.class);
        addCategoryUseCase = mock(AddCategoryUseCase.class);
        deleteCategoryUseCase = mock(DeleteCategoryUseCase.class);

        when(getAllWalletsUseCase.execute()).thenReturn(new MutableLiveData<List<WalletModel>>(Collections.emptyList()));
        viewModel = new AddTransactionViewModel(
                addTransactionUseCase,
                getAllWalletsUseCase,
                getCategoriesUseCase,
                addCategoryUseCase,
                deleteCategoryUseCase
        );
    }

    @Test
    public void parseVoiceInput_extractsVietnameseThousandSuffixAndCleansNote() {
        ParsedVoiceExpense parsed = AddTransactionViewModel.parseVoiceInput("Ăn trưa phở bò 45k", null);

        assertEquals(45_000.0, parsed.amount, 0.01);
        assertEquals("EXPENSE", parsed.type);
        assertEquals("Food", parsed.category);
        assertEquals("Ăn trưa phở bò", parsed.note);
        assertEquals(0, parsed.dayOffset);
    }

    @Test
    public void parseVoiceInput_extractsCompoundMillionAndColloquialAmounts() {
        ParsedVoiceExpense utilities = AddTransactionViewModel.parseVoiceInput("Đóng tiền điện nước 1 triệu 2", null);
        assertEquals(1_200_000.0, utilities.amount, 0.01);
        assertEquals("EXPENSE", utilities.type);
        assertEquals("Utilities", utilities.category);
        assertEquals("Đóng tiền điện nước", utilities.note);

        ParsedVoiceExpense shoes = AddTransactionViewModel.parseVoiceInput("Mua giày 1 triệu rưỡi", null);
        assertEquals(1_500_000.0, shoes.amount, 0.01);
        assertEquals("EXPENSE", shoes.type);
        assertEquals("Shopping", shoes.category);
        assertEquals("Mua giày", shoes.note);

        ParsedVoiceExpense lunch = AddTransactionViewModel.parseVoiceInput("Ăn trưa trăm rưỡi", null);
        assertEquals(150_000.0, lunch.amount, 0.01);
        assertEquals("EXPENSE", lunch.type);
        assertEquals("Food", lunch.category);
        assertEquals("Ăn trưa", lunch.note);
    }

    @Test
    public void parseVoiceInput_detectsIncomeAndSalaryCategory() {
        ParsedVoiceExpense parsed = AddTransactionViewModel.parseVoiceInput("Nhận lương tháng 15 triệu", null);

        assertEquals(15_000_000.0, parsed.amount, 0.01);
        assertEquals("INCOME", parsed.type);
        assertEquals("Salary", parsed.category);
        assertEquals("Nhận lương tháng", parsed.note);
    }

    @Test
    public void parseVoiceInput_resolvesTypeByFirstActionVerb() {
        ParsedVoiceExpense incomeFirst = AddTransactionViewModel.parseVoiceInput("Nhận 45k tiền mua cà phê hộ", null);
        assertEquals(45_000.0, incomeFirst.amount, 0.01);
        assertEquals("INCOME", incomeFirst.type);
        assertEquals("Other", incomeFirst.category);
        assertEquals("Nhận tiền mua cà phê hộ", incomeFirst.note);

        ParsedVoiceExpense buyFirst = AddTransactionViewModel.parseVoiceInput("Mua đồ nhận tại nhà 200k", null);
        assertEquals(200_000.0, buyFirst.amount, 0.01);
        assertEquals("EXPENSE", buyFirst.type);
        assertEquals("Shopping", buyFirst.category);
        assertEquals("Mua đồ nhận tại nhà", buyFirst.note);
    }

    @Test
    public void parseVoiceInput_extractsRelativeDateAndCleansNote() {
        ParsedVoiceExpense yesterday = AddTransactionViewModel.parseVoiceInput("Hôm qua mua cà phê 45k", null);
        assertEquals(45_000.0, yesterday.amount, 0.01);
        assertEquals("EXPENSE", yesterday.type);
        assertEquals("Food", yesterday.category);
        assertEquals("Mua cà phê", yesterday.note);
        assertEquals(-1, yesterday.dayOffset);

        ParsedVoiceExpense twoDaysAgo = AddTransactionViewModel.parseVoiceInput("Hôm kia thuê nhà 3 triệu", null);
        assertEquals(3_000_000.0, twoDaysAgo.amount, 0.01);
        assertEquals("EXPENSE", twoDaysAgo.type);
        assertEquals("Utilities", twoDaysAgo.category);
        assertEquals(-2, twoDaysAgo.dayOffset);
    }

    @Test
    public void parseVoiceInput_matchesCustomUserCategory() {
        List<CategoryModel> customCategories = Collections.singletonList(
                new CategoryModel(10L, "Thú cưng", "EXPENSE", "#FF9800", "custom")
        );

        ParsedVoiceExpense parsed = AddTransactionViewModel.parseVoiceInput(
                "Mua hạt cho thú cưng 250 nghìn",
                customCategories
        );

        assertEquals(250_000.0, parsed.amount, 0.01);
        assertEquals("EXPENSE", parsed.type);
        assertEquals("Thú cưng", parsed.category);
        assertEquals("Mua hạt cho thú cưng", parsed.note);
    }

    @Test
    public void saveExpense_delegatesToAddTransactionUseCase() {
        viewModel.saveExpense(65_000.0, "", "Food", null, "Bún bò", 1L, "EXPENSE");

        ArgumentCaptor<TransactionModel> captor = ArgumentCaptor.forClass(TransactionModel.class);
        verify(addTransactionUseCase).execute(captor.capture());

        TransactionModel saved = captor.getValue();
        assertEquals(65_000.0, saved.getAmount(), 0.01);
        assertEquals("Food", saved.getCategory());
        assertEquals("Bún bò", saved.getNote());
        assertEquals(1L, saved.getWalletId());
        assertEquals("EXPENSE", saved.getType());
    }

    @Test
    public void deleteCategory_delegatesToUseCase() {
        viewModel.deleteCategory(42L);
        verify(deleteCategoryUseCase).execute(42L);
    }
}

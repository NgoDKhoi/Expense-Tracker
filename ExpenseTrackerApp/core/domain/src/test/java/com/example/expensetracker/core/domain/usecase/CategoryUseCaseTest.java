package com.example.expensetracker.core.domain.usecase;

import androidx.lifecycle.MutableLiveData;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.repository.ICategoryRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CategoryUseCaseTest {

    @Mock
    private ICategoryRepository mockRepository;

    private GetCategoriesUseCase getCategoriesUseCase;
    private AddCategoryUseCase addCategoryUseCase;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        getCategoriesUseCase = new GetCategoriesUseCase(mockRepository);
        addCategoryUseCase = new AddCategoryUseCase(mockRepository);
    }

    @Test
    public void testGetCategories_WithTypeExpense_CallsRepositoryGetCategoriesByType() {
        List<CategoryModel> list = new ArrayList<>();
        list.add(new CategoryModel("Food", "EXPENSE", "#4CAF50", "restaurant"));
        MutableLiveData<List<CategoryModel>> liveData = new MutableLiveData<>(list);

        when(mockRepository.getCategoriesByType("EXPENSE")).thenReturn(liveData);

        assertNotNull(getCategoriesUseCase.execute("EXPENSE"));
        verify(mockRepository).getCategoriesByType("EXPENSE");
    }

    @Test
    public void testGetCategories_WithNullType_CallsRepositoryGetAll() {
        MutableLiveData<List<CategoryModel>> liveData = new MutableLiveData<>(new ArrayList<>());
        when(mockRepository.getAllCategories()).thenReturn(liveData);

        assertNotNull(getCategoriesUseCase.execute(null));
        verify(mockRepository).getAllCategories();
    }

    @Test
    public void testAddCategory_CallsRepositoryInsertAndReturnsSuccess() {
        CategoryModel model = new CategoryModel("Shopping", "EXPENSE", "#9C27B0", "shopping_bag");
        Result<Void> result = addCategoryUseCase.execute(model);

        assertTrue(result.isSuccess());
        verify(mockRepository).insertCategory(model);
    }
}

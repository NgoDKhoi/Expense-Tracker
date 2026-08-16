package com.example.expensetracker.core.domain.usecase;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
// removed import

import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GetAllTransactionsUseCaseTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private ITransactionRepository mockRepository;

    private GetAllTransactionsUseCase useCase;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        useCase = new GetAllTransactionsUseCase(mockRepository);
    }

    @Test
    public void testExecute_ReturnsLiveDataFromRepository() {
        // Arrange
        androidx.lifecycle.LiveData<List<TransactionModel>> fakeLiveData = org.mockito.Mockito.mock(androidx.lifecycle.LiveData.class);
        when(mockRepository.getAllExpenses()).thenReturn(fakeLiveData);

        // Act
        var result = useCase.execute();

        // Assert
        assertEquals(fakeLiveData, result);
    }
}

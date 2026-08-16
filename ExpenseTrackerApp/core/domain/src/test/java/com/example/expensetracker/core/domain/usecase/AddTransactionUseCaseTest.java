package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

public class AddTransactionUseCaseTest {

    @Mock
    private ITransactionRepository mockRepository;

    private AddTransactionUseCase useCase;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        useCase = new AddTransactionUseCase(mockRepository);
    }

    @Test
    public void testExecute_ValidTransaction_CallsRepositoryAndReturnsSuccess() {
        // Arrange
        TransactionModel transaction = new TransactionModel();
        transaction.setAmount(150.0);
        transaction.setCategory("Food");
        transaction.setWalletId(1);

        // Act
        Result<Void> result = useCase.execute(transaction);

        // Assert
        assertTrue(result.isSuccess());
        
        ArgumentCaptor<TransactionModel> txCaptor = ArgumentCaptor.forClass(TransactionModel.class);
        ArgumentCaptor<Double> amountCaptor = ArgumentCaptor.forClass(Double.class);
        
        verify(mockRepository).insertExpenseAndUpdateWallet(txCaptor.capture(), amountCaptor.capture());
        
        assertEquals(150.0, txCaptor.getValue().getAmount(), 0.001);
        assertEquals(-150.0, amountCaptor.getValue(), 0.001);
    }
}

package com.example.expensetracker.viewmodel;

import javax.inject.Inject;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;

import java.util.Date;
import java.util.List;

/**
 * ViewModel for the Add Expense screen.
 * Receives dependencies via constructor injection (no Android Context).
 */
public class AddTransactionViewModel extends ViewModel {

    private static final String TAG = "AddTransactionViewModel";

    private final AddTransactionUseCase addTransactionUseCase;
    private final GetAllWalletsUseCase getAllWalletsUseCase;
    private final LiveData<List<WalletModel>> wallets;

    @Inject
    public AddTransactionViewModel(
            AddTransactionUseCase addTransactionUseCase,
            GetAllWalletsUseCase getAllWalletsUseCase) {

        Log.d(TAG, "AddTransactionViewModel created with injected use cases.");

        this.addTransactionUseCase = addTransactionUseCase;
        this.getAllWalletsUseCase = getAllWalletsUseCase;
        this.wallets = getAllWalletsUseCase.execute();
    }

    public LiveData<List<WalletModel>> getWallets() {
        return wallets;
    }

    /**
     * Saves an expense record using the AddTransactionUseCase.
     * The use case handles database insertion and wallet balance update.
     */
    public void saveExpense(double amount, String title, String category,
                            String imageUri, String note, long walletId, String type) {
        Log.d(TAG, "saveExpense: title=" + title + ", amount=" + amount
                + ", category=" + category + ", type=" + type);

        TransactionModel record = new TransactionModel.Builder()
                .amount(amount)
                .title(title)
                .category(category)
                .imageUri(imageUri)
                .timestamp(new Date())
                .syncStatus("PENDING")
                .note(note)
                .walletId(walletId)
                .type(type)
                .build();

        addTransactionUseCase.execute(record);
    }
}


package com.example.expensetracker.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.usecase.AddCategoryUseCase;
import com.example.expensetracker.core.domain.usecase.AddTransactionUseCase;
import com.example.expensetracker.core.domain.usecase.GetAllWalletsUseCase;
import com.example.expensetracker.core.domain.usecase.GetCategoriesUseCase;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

/**
 * ViewModel for the Add Expense screen.
 * Receives dependencies via constructor injection (no Android Context).
 */
public class AddTransactionViewModel extends ViewModel {

    private static final String TAG = "AddTransactionVM";

    private final AddTransactionUseCase addTransactionUseCase;
    private final GetAllWalletsUseCase getAllWalletsUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final AddCategoryUseCase addCategoryUseCase;
    private final LiveData<List<WalletModel>> wallets;

    @Inject
    public AddTransactionViewModel(
            AddTransactionUseCase addTransactionUseCase,
            GetAllWalletsUseCase getAllWalletsUseCase,
            GetCategoriesUseCase getCategoriesUseCase,
            AddCategoryUseCase addCategoryUseCase) {

        Log.d(TAG, "AddTransactionViewModel created with injected use cases.");

        this.addTransactionUseCase = addTransactionUseCase;
        this.getAllWalletsUseCase = getAllWalletsUseCase;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.addCategoryUseCase = addCategoryUseCase;
        this.wallets = getAllWalletsUseCase.execute();
    }

    public LiveData<List<WalletModel>> getWallets() {
        return wallets;
    }

    public LiveData<List<CategoryModel>> getCategories(String type) {
        return getCategoriesUseCase.execute(type);
    }

    public void addCategory(String name, String type, String colorHex, String iconName) {
        addCategoryUseCase.execute(new CategoryModel(name, type, colorHex, iconName));
    }

    public void saveExpense(double amount, String title, String category,
                            String imageUri, String note, long walletId, String type) {
        saveExpense(amount, title, category, imageUri, note, walletId, type, new Date());
    }

    public void saveExpense(double amount, String title, String category,
                            String imageUri, String note, long walletId, String type, Date timestamp) {
        Log.d(TAG, "saveExpense: title=" + title + ", amount=" + amount
                + ", category=" + category + ", type=" + type + ", note=" + note);

        TransactionModel record = new TransactionModel.Builder()
                .amount(amount)
                .title(title)
                .category(category)
                .imageUri(imageUri)
                .timestamp(timestamp != null ? timestamp : new Date())
                .syncStatus("PENDING")
                .note(note)
                .walletId(walletId)
                .type(type)
                .build();

        addTransactionUseCase.execute(record);
    }
}

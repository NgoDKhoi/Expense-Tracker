package com.example.expensetracker.core.database.di;

import android.content.Context;
import com.example.expensetracker.core.database.ExpenseDatabase;
import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class DatabaseModule {

    @Provides
    @Singleton
    ExpenseDatabase provideDatabase(Context context) {
        return ExpenseDatabase.getInstance(context);
    }

    @Provides
    TransactionDao provideTransactionDao(ExpenseDatabase database) {
        return database.transactionDao();
    }

    @Provides
    WalletDao provideWalletDao(ExpenseDatabase database) {
        return database.walletDao();
    }
}

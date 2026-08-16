package com.example.expensetracker.core.database.di;

import com.example.expensetracker.core.database.repository.TransactionRepositoryImpl;
import com.example.expensetracker.core.database.repository.WalletRepositoryImpl;
import com.example.expensetracker.core.domain.repository.ITransactionRepository;
import com.example.expensetracker.core.domain.repository.IWalletRepository;
import dagger.Binds;
import dagger.Module;
import javax.inject.Singleton;

@Module
public abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract ITransactionRepository bindTransactionRepository(TransactionRepositoryImpl impl);

    @Binds
    @Singleton
    abstract IWalletRepository bindWalletRepository(WalletRepositoryImpl impl);
}

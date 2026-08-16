package com.example.expensetracker.di;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.expensetracker.viewmodel.AddTransactionViewModel;
import com.example.expensetracker.viewmodel.HomeViewModel;
import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
public abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel.class)
    abstract ViewModel bindHomeViewModel(HomeViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(AddTransactionViewModel.class)
    abstract ViewModel bindAddTransactionViewModel(AddTransactionViewModel viewModel);

    @Binds
    abstract ViewModelProvider.Factory bindViewModelFactory(DaggerViewModelFactory factory);
}

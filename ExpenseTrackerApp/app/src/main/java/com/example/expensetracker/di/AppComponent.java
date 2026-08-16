package com.example.expensetracker.di;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.MainActivity;
import com.example.expensetracker.core.database.di.DatabaseModule;
import com.example.expensetracker.core.database.di.RepositoryModule;
import com.example.expensetracker.ui.AddTransactionFragment;
import com.example.expensetracker.ui.AiAssistantFragment;
import com.example.expensetracker.ui.BudgetFragment;
import com.example.expensetracker.ui.CameraHostFragment;
import com.example.expensetracker.ui.DashboardFragment;
import com.example.expensetracker.ui.DashboardOverviewFragment;
import com.example.expensetracker.ui.HistoryFragment;
import com.example.expensetracker.ui.PhotosFragment;
import com.example.expensetracker.ui.ReceiptDetailFragment;
import com.example.expensetracker.ui.SettingsFragment;
import dagger.Component;
import javax.inject.Singleton;

@Singleton
@Component(modules = {
        AppModule.class,
        DatabaseModule.class,
        RepositoryModule.class,
        ViewModelModule.class
})
public interface AppComponent {
    void inject(ExpenseTrackerApplication application);
    void inject(MainActivity activity);
    
    // Fragments
    void inject(AddTransactionFragment fragment);
    void inject(AiAssistantFragment fragment);
    void inject(BudgetFragment fragment);
    void inject(CameraHostFragment fragment);
    void inject(DashboardFragment fragment);
    void inject(DashboardOverviewFragment fragment);
    void inject(HistoryFragment fragment);
    void inject(PhotosFragment fragment);
    void inject(ReceiptDetailFragment fragment);
    void inject(SettingsFragment fragment);
}

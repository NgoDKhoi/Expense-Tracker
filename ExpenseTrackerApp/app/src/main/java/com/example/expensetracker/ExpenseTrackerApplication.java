package com.example.expensetracker;

import android.app.Application;
import android.util.Log;

import com.example.expensetracker.di.AppComponent;
import com.example.expensetracker.di.AppModule;
import com.example.expensetracker.di.DaggerAppComponent;
import com.example.expensetracker.sync.NetworkChangeReceiver;
import com.example.expensetracker.sync.WorkManagerSyncScheduler;

public class ExpenseTrackerApplication extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        appComponent.inject(this);

        WorkManagerSyncScheduler scheduler = appComponent.syncScheduler();
        scheduler.schedulePeriodic();
        NetworkChangeReceiver.register(this, scheduler);
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}

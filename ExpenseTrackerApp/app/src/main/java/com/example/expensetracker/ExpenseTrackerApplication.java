package com.example.expensetracker;

import android.app.Application;
import android.util.Log;

import com.example.expensetracker.di.AppComponent;
import com.example.expensetracker.di.AppModule;
import com.example.expensetracker.di.DaggerAppComponent;

public class ExpenseTrackerApplication extends Application {

    private AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = DaggerAppComponent.builder()
                .appModule(new AppModule(this))
                .build();
        appComponent.inject(this);
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }
}

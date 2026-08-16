package com.example.expensetracker.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;

@Module
public class AppModule {
    private final Context context;

    public AppModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context provideContext() {
        return context;
    }

    @Provides
    @Singleton
    java.util.concurrent.ExecutorService provideExecutorService() {
        return java.util.concurrent.Executors.newFixedThreadPool(4);
    }
}

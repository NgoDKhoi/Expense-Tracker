package com.example.expensetracker.sync;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.core.domain.usecase.SyncDataUseCase;

/** Runs one sync. WorkManager retries with backoff when it fails (offline, server down, ...). */
public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";
    private static final int MAX_ATTEMPTS = 5;

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncDataUseCase sync = ((ExpenseTrackerApplication) getApplicationContext()).getAppComponent().syncDataUseCase();
        com.example.expensetracker.core.domain.Result<Void> result = sync.execute();
        if (result.isSuccess()) return Result.success();
        Log.w(TAG, "Sync failed (attempt " + getRunAttemptCount() + "): " + result.getMessage());
        // After a few tries give up; the next write, app start or periodic run tries again.
        return getRunAttemptCount() + 1 < MAX_ATTEMPTS ? Result.retry() : Result.failure();
    }
}

package com.example.expensetracker.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.expensetracker.core.domain.repository.ISyncScheduler;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WorkManagerSyncScheduler implements ISyncScheduler {
    private static final String ONE_OFF = "sync";
    private static final String PERIODIC = "sync-periodic";
    // Short wait so a burst of edits (or delete + undo) becomes one sync.
    private static final long DELAY_SECONDS = 5;

    private final Context context;

    @Inject
    public WorkManagerSyncScheduler(Context context) {
        this.context = context;
    }

    private static Constraints online() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    @Override
    public void requestSync() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(online())
                .setInitialDelay(DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        // REPLACE restarts the delay; SyncDataUseCase is locked, so a replaced run never overlaps the new one.
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_OFF, ExistingWorkPolicy.REPLACE, request);
    }

    @Override
    public void cancelAll() {
        WorkManager wm = WorkManager.getInstance(context);
        wm.cancelUniqueWork(ONE_OFF);
        wm.cancelUniqueWork(PERIODIC);
    }

    /** Background sync every 6 hours while online. KEEP: calling it on every app start is fine. */
    public void schedulePeriodic() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(online())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request);
    }
}

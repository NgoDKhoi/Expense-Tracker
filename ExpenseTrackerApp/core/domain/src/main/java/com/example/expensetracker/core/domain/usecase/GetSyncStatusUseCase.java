package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.repository.ISyncLocal;

import javax.inject.Inject;

/** Read-only sync facts for the UI. Blocks on the database; run it off the main thread. */
public class GetSyncStatusUseCase {
    private final ISyncLocal local;

    @Inject
    public GetSyncStatusUseCase(ISyncLocal local) {
        this.local = local;
    }

    /** Changes not yet backed up. */
    public int pendingChanges() {
        return local.countPendingChanges();
    }

    /** Expenses on this device that belong to no account yet (asked about after sign in). */
    public int unsyncedTransactions() {
        return local.countUnsyncedTransactions();
    }

    /** Epoch millis; 0 if never synced. */
    public long lastSyncedAt() {
        return local.getLastSyncedAt();
    }
}

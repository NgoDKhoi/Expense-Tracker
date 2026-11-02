package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ISyncScheduler;
import javax.inject.Inject;

/** Signs out and removes this account's data from the device. Blocks; run it off the main thread. */
public class SignOutUseCase {
    private final IAuthRepository repository;
    private final ISyncScheduler syncScheduler;
    private final SyncDataUseCase syncData;

    @Inject
    public SignOutUseCase(IAuthRepository repository, ISyncScheduler syncScheduler, SyncDataUseCase syncData) {
        this.repository = repository;
        this.syncScheduler = syncScheduler;
        this.syncData = syncData;
    }

    public void execute() {
        // Session first: a sync that starts now sees a guest and does nothing.
        repository.signOut();
        syncScheduler.cancelAll();
        syncData.clearLocalData();
    }
}

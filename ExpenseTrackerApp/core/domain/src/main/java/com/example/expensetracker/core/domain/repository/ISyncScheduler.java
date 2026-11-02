package com.example.expensetracker.core.domain.repository;

/** Runs sync in the background. Safe to call from any thread and as often as needed. */
public interface ISyncScheduler {
    /** Sync soon (a few seconds, once online). Repeated calls are merged into one run. */
    void requestSync();

    /** Cancel queued and periodic syncs (after sign out). */
    void cancelAll();
}

package com.example.expensetracker.core.domain.model;

import java.util.List;

/** One page of rows pulled from the server, plus the server time of the newest row (the next cursor). */
public class SyncPage<T> {
    private final List<T> rows;
    private final long maxSyncedAt;

    public SyncPage(List<T> rows, long maxSyncedAt) {
        this.rows = rows;
        this.maxSyncedAt = maxSyncedAt;
    }

    public List<T> getRows() { return rows; }

    /** Epoch millis of the last row's server timestamp; 0 when the page is empty. */
    public long getMaxSyncedAt() { return maxSyncedAt; }
}

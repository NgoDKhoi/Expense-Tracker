package com.example.expensetracker.core.database.entity;

import androidx.room.ColumnInfo;

import java.util.UUID;

/** Columns every synced table shares. Room maps these superclass fields into each table. */
public abstract class SyncableEntity {
    private String uuid;
    @ColumnInfo(defaultValue = "0")
    private long updatedAt;
    @ColumnInfo(defaultValue = "0")
    private boolean deleted;
    private SyncStatus syncStatus = SyncStatus.PENDING;

    /** Call before every local write so the row is pushed on the next sync. */
    public void markDirty() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        updatedAt = System.currentTimeMillis();
        syncStatus = SyncStatus.PENDING;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public SyncStatus getSyncStatus() { return syncStatus; }
    public void setSyncStatus(SyncStatus syncStatus) { this.syncStatus = syncStatus; }
}

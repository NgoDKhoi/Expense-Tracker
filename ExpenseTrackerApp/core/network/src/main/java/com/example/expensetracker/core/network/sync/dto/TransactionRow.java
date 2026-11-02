package com.example.expensetracker.core.network.sync.dto;

import com.google.gson.annotations.SerializedName;

/** A row of public.transactions (supabase/sync_schema.sql). user_id is filled by the server. */
public class TransactionRow {
    public String id;
    @SerializedName("wallet_id")
    public String walletId;
    public double amount;
    public String type;
    public String category;
    public String title;
    public String note;
    /** Epoch millis; the app's "timestamp". */
    @SerializedName("occurred_at")
    public Long occurredAt;
    @SerializedName("updated_at")
    public long updatedAt;
    public boolean deleted;
    /** Server-only; ignored on push. */
    @SerializedName("synced_at")
    public String syncedAt;
}

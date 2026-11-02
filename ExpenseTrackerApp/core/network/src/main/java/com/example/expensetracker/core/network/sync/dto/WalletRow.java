package com.example.expensetracker.core.network.sync.dto;

import com.google.gson.annotations.SerializedName;

/** A row of public.wallets (supabase/sync_schema.sql). user_id is filled by the server. */
public class WalletRow {
    public String id;
    public String name;
    @SerializedName("initial_balance")
    public double initialBalance;
    @SerializedName("color_hex")
    public String colorHex;
    @SerializedName("icon_name")
    public String iconName;
    @SerializedName("updated_at")
    public long updatedAt;
    public boolean deleted;
    /** Server-only; ignored on push. */
    @SerializedName("synced_at")
    public String syncedAt;
}

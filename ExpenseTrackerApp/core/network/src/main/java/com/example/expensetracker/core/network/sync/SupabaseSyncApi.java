package com.example.expensetracker.core.network.sync;

import com.example.expensetracker.core.network.sync.dto.CategoryRow;
import com.example.expensetracker.core.network.sync.dto.TransactionRow;
import com.example.expensetracker.core.network.sync.dto.WalletRow;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * PostgREST endpoints for the sync tables. The apikey header is added by NetworkModule.
 * Push: bulk upsert on (user_id, id). "columns" limits the insert to the client's columns,
 * so user_id and synced_at always come from the server.
 * Pull: rows with synced_at after a cursor, oldest first. synced = "gt.<ISO time>".
 */
public interface SupabaseSyncApi {
    String UPSERT = "Prefer: resolution=merge-duplicates,return=minimal";

    @Headers(UPSERT)
    @POST("rest/v1/wallets?on_conflict=user_id,id&columns=id,name,initial_balance,color_hex,icon_name,updated_at,deleted")
    Call<Void> upsertWallets(@Header("Authorization") String bearer, @Body List<WalletRow> rows);

    @Headers(UPSERT)
    @POST("rest/v1/categories?on_conflict=user_id,id&columns=id,name,type,color_hex,icon_name,updated_at,deleted")
    Call<Void> upsertCategories(@Header("Authorization") String bearer, @Body List<CategoryRow> rows);

    @Headers(UPSERT)
    @POST("rest/v1/transactions?on_conflict=user_id,id&columns=id,wallet_id,amount,type,category,title,note,occurred_at,updated_at,deleted")
    Call<Void> upsertTransactions(@Header("Authorization") String bearer, @Body List<TransactionRow> rows);

    @GET("rest/v1/wallets?order=synced_at.asc")
    Call<List<WalletRow>> pullWallets(@Header("Authorization") String bearer, @Query("synced_at") String synced, @Query("limit") int limit);

    @GET("rest/v1/categories?order=synced_at.asc")
    Call<List<CategoryRow>> pullCategories(@Header("Authorization") String bearer, @Query("synced_at") String synced, @Query("limit") int limit);

    @GET("rest/v1/transactions?order=synced_at.asc")
    Call<List<TransactionRow>> pullTransactions(@Header("Authorization") String bearer, @Query("synced_at") String synced, @Query("limit") int limit);
}

package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;

import java.util.List;

/** The on-device side of sync. All calls block; run them off the main thread. */
public interface ISyncLocal {
    enum Table { WALLETS, CATEGORIES, TRANSACTIONS }

    List<WalletModel> getPendingWallets();
    List<CategoryModel> getPendingCategories();
    /** Pending transactions with walletUuid filled; rows whose wallet no longer exists are left out. */
    List<TransactionModel> getPendingTransactions();

    /** Marks the row SYNCED, but only if it was not edited again since (same updatedAt). */
    void markSynced(Table table, String uuid, long updatedAt);

    /** updatedAt of the local row if it still waits to be pushed, else null (missing or already synced). */
    Long getPendingUpdatedAt(Table table, String uuid);

    /** Insert or overwrite by uuid with a server row, as SYNCED. A transaction whose wallet is unknown is skipped. */
    void saveRemoteWallet(WalletModel wallet);
    void saveRemoteCategory(CategoryModel category);
    void saveRemoteTransaction(TransactionModel transaction);

    /** Deletes soft-deleted rows the server already has. */
    void purgeSyncedDeletes();

    /** Server time (epoch millis) of the newest row pulled so far; 0 before the first pull. */
    long getCursor(Table table);
    void setCursor(Table table, long syncedAtMillis);
}

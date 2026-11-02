package com.example.expensetracker.core.domain.repository;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.SyncPage;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;

import java.util.List;

/**
 * The server copy of the user's data. Rows are matched by uuid; the local id is never sent.
 * Push = upsert (the server keeps the newer updatedAt). Pull = rows changed on the server after a cursor,
 * oldest first, at most {@code limit} per call.
 * Network calls block, so callers must run them off the main thread.
 */
public interface ISyncRemote {
    Result<Void> pushWallets(String accessToken, List<WalletModel> wallets);
    Result<Void> pushCategories(String accessToken, List<CategoryModel> categories);
    /** Each transaction must have walletUuid set. */
    Result<Void> pushTransactions(String accessToken, List<TransactionModel> transactions);

    Result<SyncPage<WalletModel>> pullWallets(String accessToken, long sinceMillis, int limit);
    Result<SyncPage<CategoryModel>> pullCategories(String accessToken, long sinceMillis, int limit);
    /** Pulled transactions carry walletUuid; walletId is left 0 for the caller to resolve. */
    Result<SyncPage<TransactionModel>> pullTransactions(String accessToken, long sinceMillis, int limit);
}

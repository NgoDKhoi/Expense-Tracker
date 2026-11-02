package com.example.expensetracker.core.domain.usecase;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.SyncPage;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ISyncLocal;
import com.example.expensetracker.core.domain.repository.ISyncLocal.Table;
import com.example.expensetracker.core.domain.repository.ISyncRemote;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToLongFunction;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Two-way sync for the signed-in user. Blocks; run it off the main thread.
 * 1. Push local PENDING rows (wallets, then categories, then transactions, so foreign keys exist).
 * 2. Pull rows changed on the server since the last cursor. Newer updatedAt wins; a local
 *    row still waiting to be pushed and newer than the server's is kept.
 * 3. Drop soft-deleted rows the server already has.
 * Guests are skipped. Any error stops the run; the next run picks up where this one stopped.
 */
@Singleton
public class SyncDataUseCase {
    static final int PAGE_SIZE = 500;
    // Pull a few seconds before the cursor on the first page (see the shortcut note in supabase/sync_schema.sql).
    static final long PULL_OVERLAP_MS = 5_000;

    private final IAuthRepository auth;
    private final ISyncLocal local;
    private final ISyncRemote remote;

    @Inject
    public SyncDataUseCase(IAuthRepository auth, ISyncLocal local, ISyncRemote remote) {
        this.auth = auth;
        this.local = local;
        this.remote = remote;
    }

    /** Push lists are sent in chunks of this size. */
    private interface Push<T> {
        Result<Void> send(String token, List<T> rows);
    }

    private interface Pull<T> {
        Result<SyncPage<T>> fetch(String token, long since, int limit);
    }

    // synchronized + @Singleton: the periodic run and an on-demand run never overlap.
    public synchronized Result<Void> execute() {
        Result<String> token = auth.getValidAccessToken();
        if (!token.isSuccess()) return Result.error(token.getMessage(), token.getException());
        String t = token.getData();
        if (t == null) return Result.success(null); // guest: nothing to sync

        Result<Void> r = push(t, Table.WALLETS, local.getPendingWallets(), remote::pushWallets,
                m -> m.getUuid(), m -> m.getUpdatedAt());
        if (r.isSuccess()) r = push(t, Table.CATEGORIES, local.getPendingCategories(), remote::pushCategories,
                m -> m.getUuid(), m -> m.getUpdatedAt());
        if (r.isSuccess()) r = push(t, Table.TRANSACTIONS, local.getPendingTransactions(), remote::pushTransactions,
                m -> m.getUuid(), m -> m.getUpdatedAt());
        if (!r.isSuccess()) return r;
        local.purgeSyncedDeletes();

        r = pull(t, Table.WALLETS, remote::pullWallets, local::saveRemoteWallet, m -> m.getUuid(), m -> m.getUpdatedAt());
        if (r.isSuccess()) r = pull(t, Table.CATEGORIES, remote::pullCategories, local::saveRemoteCategory,
                m -> m.getUuid(), m -> m.getUpdatedAt());
        if (r.isSuccess()) r = pull(t, Table.TRANSACTIONS, remote::pullTransactions, local::saveRemoteTransaction,
                m -> m.getUuid(), m -> m.getUpdatedAt());
        if (!r.isSuccess()) return r;
        local.purgeSyncedDeletes();
        local.setLastSyncedAt(System.currentTimeMillis());
        return Result.success(null);
    }

    /** Wipes local data. Shares the sync lock, so it never runs in the middle of a sync. */
    public synchronized void clearLocalData() {
        local.clearAll();
    }

    private <T> Result<Void> push(String token, Table table, List<T> rows, Push<T> send,
                                  Function<T, String> uuid, ToLongFunction<T> updatedAt) {
        for (int from = 0; from < rows.size(); from += PAGE_SIZE) {
            List<T> chunk = rows.subList(from, Math.min(from + PAGE_SIZE, rows.size()));
            Result<Void> r = send.send(token, chunk);
            if (!r.isSuccess()) return r;
            for (T row : chunk) local.markSynced(table, uuid.apply(row), updatedAt.applyAsLong(row));
        }
        return Result.success(null);
    }

    private <T> Result<Void> pull(String token, Table table, Pull<T> fetch, Consumer<T> save,
                                  Function<T, String> uuid, ToLongFunction<T> updatedAt) {
        long cursor = local.getCursor(table);
        long since = Math.max(0, cursor - PULL_OVERLAP_MS);
        while (true) {
            Result<SyncPage<T>> page = fetch.fetch(token, since, PAGE_SIZE);
            if (!page.isSuccess()) return Result.error(page.getMessage(), page.getException());
            List<T> rows = page.getData().getRows();
            for (T row : rows) {
                Long pendingAt = local.getPendingUpdatedAt(table, uuid.apply(row));
                boolean localIsNewer = pendingAt != null && pendingAt > updatedAt.applyAsLong(row);
                if (!localIsNewer) save.accept(row);
            }
            if (rows.isEmpty()) break;
            cursor = Math.max(cursor, page.getData().getMaxSyncedAt());
            local.setCursor(table, cursor);
            if (rows.size() < PAGE_SIZE) break;
            since = page.getData().getMaxSyncedAt(); // later pages: no overlap, or a full page could repeat forever
        }
        return Result.success(null);
    }
}

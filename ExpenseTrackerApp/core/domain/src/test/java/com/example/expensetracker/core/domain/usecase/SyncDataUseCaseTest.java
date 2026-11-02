package com.example.expensetracker.core.domain.usecase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.expensetracker.core.domain.Result;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.SyncPage;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.IAuthRepository;
import com.example.expensetracker.core.domain.repository.ISyncLocal;
import com.example.expensetracker.core.domain.repository.ISyncLocal.Table;
import com.example.expensetracker.core.domain.repository.ISyncRemote;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SyncDataUseCaseTest {
    private IAuthRepository auth;
    private ISyncLocal local;
    private ISyncRemote remote;
    private SyncDataUseCase sync;

    @Before
    public void setup() {
        auth = mock(IAuthRepository.class);
        local = mock(ISyncLocal.class);
        remote = mock(ISyncRemote.class);
        sync = new SyncDataUseCase(auth, local, remote);

        when(auth.getValidAccessToken()).thenReturn(Result.success("tok"));
        when(local.getPendingWallets()).thenReturn(new ArrayList<>());
        when(local.getPendingCategories()).thenReturn(new ArrayList<>());
        when(local.getPendingTransactions()).thenReturn(new ArrayList<>());
        when(remote.pushWallets(anyString(), any())).thenReturn(Result.success(null));
        when(remote.pushCategories(anyString(), any())).thenReturn(Result.success(null));
        when(remote.pushTransactions(anyString(), any())).thenReturn(Result.success(null));
        when(remote.pullWallets(anyString(), anyLong(), anyInt())).thenReturn(page(Collections.emptyList(), 0));
        when(remote.pullCategories(anyString(), anyLong(), anyInt())).thenReturn(page(Collections.emptyList(), 0));
        when(remote.pullTransactions(anyString(), anyLong(), anyInt())).thenReturn(page(Collections.emptyList(), 0));
    }

    private static <T> Result<SyncPage<T>> page(List<T> rows, long max) {
        return Result.success(new SyncPage<>(rows, max));
    }

    private static WalletModel wallet(String uuid, long updatedAt) {
        WalletModel w = new WalletModel();
        w.setUuid(uuid);
        w.setUpdatedAt(updatedAt);
        return w;
    }

    private static TransactionModel tx(String uuid, long updatedAt) {
        TransactionModel t = new TransactionModel();
        t.setUuid(uuid);
        t.setUpdatedAt(updatedAt);
        return t;
    }

    @Test
    public void guest_isSkipped() {
        when(auth.getValidAccessToken()).thenReturn(Result.success(null));
        assertTrue(sync.execute().isSuccess());
        verifyNoInteractions(remote, local);
    }

    @Test
    public void tokenError_stopsWithoutCallingServer() {
        when(auth.getValidAccessToken()).thenReturn(Result.error("offline", null));
        assertFalse(sync.execute().isSuccess());
        verifyNoInteractions(remote);
    }

    @Test
    public void push_runsWalletsThenCategoriesThenTransactions_andMarksSynced() {
        List<WalletModel> wallets = new ArrayList<>(List.of(wallet("w1", 10)));
        List<TransactionModel> txs = new ArrayList<>(List.of(tx("t1", 20)));
        when(local.getPendingWallets()).thenReturn(wallets);
        when(local.getPendingTransactions()).thenReturn(txs);

        assertTrue(sync.execute().isSuccess());

        InOrder order = inOrder(remote, local);
        order.verify(remote).pushWallets("tok", wallets);
        order.verify(local).markSynced(Table.WALLETS, "w1", 10);
        order.verify(remote).pushTransactions("tok", txs);
        order.verify(local).markSynced(Table.TRANSACTIONS, "t1", 20);
        order.verify(local).purgeSyncedDeletes();
        order.verify(remote).pullWallets(eq("tok"), anyLong(), anyInt());
        // Nothing pending there: no request at all.
        verify(remote, never()).pushCategories(anyString(), any());
    }

    @Test
    public void pushError_stopsBeforeLaterTablesAndPull() {
        when(local.getPendingWallets()).thenReturn(new ArrayList<>(List.of(wallet("w1", 10))));
        when(remote.pushWallets(anyString(), any())).thenReturn(Result.error("down", null));

        assertFalse(sync.execute().isSuccess());

        verify(local, never()).markSynced(any(), anyString(), anyLong());
        verify(remote, never()).pushCategories(anyString(), any());
        verify(remote, never()).pullWallets(anyString(), anyLong(), anyInt());
    }

    @Test
    public void pull_newerEditWins() {
        TransactionModel olderThanLocal = tx("keep-local", 100);
        TransactionModel newerThanLocal = tx("take-remote", 300);
        TransactionModel notPendingHere = tx("not-pending", 50);
        when(remote.pullTransactions(anyString(), anyLong(), anyInt()))
                .thenReturn(page(List.of(olderThanLocal, newerThanLocal, notPendingHere), 1000));
        when(local.getPendingUpdatedAt(Table.TRANSACTIONS, "keep-local")).thenReturn(200L);
        when(local.getPendingUpdatedAt(Table.TRANSACTIONS, "take-remote")).thenReturn(200L);
        when(local.getPendingUpdatedAt(Table.TRANSACTIONS, "not-pending")).thenReturn(null);

        assertTrue(sync.execute().isSuccess());

        verify(local, never()).saveRemoteTransaction(olderThanLocal);
        verify(local).saveRemoteTransaction(newerThanLocal);
        verify(local).saveRemoteTransaction(notPendingHere);
        verify(local).setCursor(Table.TRANSACTIONS, 1000);
    }

    @Test
    public void pull_overlapsOnlyTheFirstPage_andFollowsFullPages() {
        when(local.getCursor(Table.WALLETS)).thenReturn(100_000L);
        List<WalletModel> full = new ArrayList<>();
        for (int i = 0; i < SyncDataUseCase.PAGE_SIZE; i++) full.add(wallet("w" + i, 1));
        long firstSince = 100_000L - SyncDataUseCase.PULL_OVERLAP_MS;
        when(remote.pullWallets("tok", firstSince, SyncDataUseCase.PAGE_SIZE)).thenReturn(page(full, 200_000));
        when(remote.pullWallets("tok", 200_000, SyncDataUseCase.PAGE_SIZE))
                .thenReturn(page(List.of(wallet("last", 1)), 250_000));

        assertTrue(sync.execute().isSuccess());

        verify(remote).pullWallets("tok", firstSince, SyncDataUseCase.PAGE_SIZE);
        verify(remote).pullWallets("tok", 200_000, SyncDataUseCase.PAGE_SIZE);
        verify(local).setCursor(Table.WALLETS, 250_000);
    }

    @Test
    public void pullError_returnsError() {
        when(remote.pullCategories(anyString(), anyLong(), anyInt())).thenReturn(Result.error("bad", null));
        assertFalse(sync.execute().isSuccess());
        verify(remote, never()).pullTransactions(anyString(), anyLong(), anyInt());
        verify(local, never()).saveRemoteCategory(any(CategoryModel.class));
    }
}

package com.example.expensetracker.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.TransactionType;
import com.example.expensetracker.core.database.entity.WalletEntity;
import com.example.expensetracker.core.database.repository.SyncLocalImpl;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.repository.ISyncLocal.Table;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SyncLocalTest {
    private ExpenseDatabase db;
    private SyncLocalImpl local;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, ExpenseDatabase.class).allowMainThreadQueries().build();
        local = new SyncLocalImpl(context, db);
    }

    @After
    public void tearDown() {
        db.close();
    }

    private WalletEntity addWallet(String uuid) {
        WalletEntity w = new WalletEntity();
        w.setUuid(uuid);
        w.setName("W");
        w.markDirty();
        db.walletDao().insertWallet(w);
        return db.walletDao().findByUuid(uuid);
    }

    private TransactionEntity addTx(String uuid, long walletId) {
        TransactionEntity t = new TransactionEntity();
        t.setUuid(uuid);
        t.setWalletId(walletId);
        t.setAmount(10);
        t.setType(TransactionType.EXPENSE);
        t.setTimestamp(new Date(1));
        t.setImageUri("file:///photo.jpg");
        t.markDirty();
        db.transactionDao().insertExpense(t);
        return db.transactionDao().findByUuid(uuid);
    }

    @Test
    public void pendingTransactions_carryWalletUuid_andSkipOrphans() {
        WalletEntity w = addWallet("w1");
        addTx("t1", w.getId());
        addTx("orphan", 999);

        List<TransactionModel> pending = local.getPendingTransactions();
        assertEquals(1, pending.size());
        assertEquals("t1", pending.get(0).getUuid());
        assertEquals("w1", pending.get(0).getWalletUuid());
    }

    @Test
    public void markSynced_ignoresRowsEditedSincePush() {
        WalletEntity w = addWallet("w1");
        TransactionEntity t = addTx("t1", w.getId());

        local.markSynced(Table.TRANSACTIONS, "t1", t.getUpdatedAt() - 1); // edited after the push
        assertEquals(Long.valueOf(t.getUpdatedAt()), local.getPendingUpdatedAt(Table.TRANSACTIONS, "t1"));

        local.markSynced(Table.TRANSACTIONS, "t1", t.getUpdatedAt());
        assertNull(local.getPendingUpdatedAt(Table.TRANSACTIONS, "t1"));
    }

    @Test
    public void saveRemoteTransaction_updatesByUuid_keepsPhoto_andResolvesWallet() {
        WalletEntity w = addWallet("w1");
        TransactionEntity t = addTx("t1", w.getId());

        TransactionModel remote = new TransactionModel();
        remote.setUuid("t1");
        remote.setWalletUuid("w1");
        remote.setAmount(99);
        remote.setType("EXPENSE");
        remote.setUpdatedAt(t.getUpdatedAt() + 1);
        local.saveRemoteTransaction(remote);

        TransactionEntity after = db.transactionDao().findByUuid("t1");
        assertEquals(t.getId(), after.getId());
        assertEquals(99, after.getAmount(), 0.001);
        assertEquals(w.getId(), after.getWalletId());
        assertEquals("file:///photo.jpg", after.getImageUri());
        assertNull(local.getPendingUpdatedAt(Table.TRANSACTIONS, "t1"));

        // Unknown wallet: skipped instead of creating an orphan.
        remote.setUuid("t2");
        remote.setWalletUuid("nope");
        local.saveRemoteTransaction(remote);
        assertNull(db.transactionDao().findByUuid("t2"));
    }

    @Test
    public void purge_removesOnlySyncedDeletes() {
        WalletEntity w = addWallet("w1");
        TransactionEntity pending = addTx("pending", w.getId());
        TransactionEntity synced = addTx("synced", w.getId());
        db.transactionDao().setDeleted(pending.getId(), true, 5);
        db.transactionDao().setDeleted(synced.getId(), true, 5);
        local.markSynced(Table.TRANSACTIONS, "synced", 5);

        local.purgeSyncedDeletes();

        assertTrue(db.transactionDao().findByUuid("pending").isDeleted());
        assertNull(db.transactionDao().findByUuid("synced"));
    }

    @Test
    public void defaults_areSeededWithUpdatedAtZero() {
        ExpenseDatabase.seedDefaults(db.getOpenHelper().getWritableDatabase());
        try (Cursor c = db.query("SELECT COUNT(*), MAX(updatedAt) FROM categories", null)) {
            c.moveToFirst();
            assertEquals(10, c.getInt(0));
            assertEquals(0, c.getLong(1));
        }
        assertEquals(0, db.walletDao().findByUuid(ExpenseDatabase.DEFAULT_WALLET_UUID).getUpdatedAt());
    }
}

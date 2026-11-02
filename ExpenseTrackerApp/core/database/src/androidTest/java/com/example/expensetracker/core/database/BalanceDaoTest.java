package com.example.expensetracker.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.TransactionType;
import com.example.expensetracker.core.database.entity.WalletEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
public class BalanceDaoTest {
    private ExpenseDatabase db;
    private WalletDao walletDao;
    private TransactionDao transactionDao;

    @Before
    public void setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), ExpenseDatabase.class)
                .allowMainThreadQueries()
                .build();
        walletDao = db.walletDao();
        transactionDao = db.transactionDao();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private long addWallet(double initialBalance) {
        WalletEntity w = new WalletEntity();
        w.setName("W");
        w.setInitialBalance(initialBalance);
        w.markDirty();
        walletDao.insertWallet(w);
        try (Cursor c = db.query("SELECT MAX(id) FROM wallet_table", null)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }

    private long addTx(long walletId, double amount, TransactionType type) {
        TransactionEntity t = new TransactionEntity();
        t.setWalletId(walletId);
        t.setAmount(amount);
        t.setType(type);
        t.setTimestamp(new Date());
        t.markDirty();
        transactionDao.insertExpense(t);
        try (Cursor c = db.query("SELECT MAX(id) FROM transactions", null)) {
            c.moveToFirst();
            return c.getLong(0);
        }
    }

    private double balance(long walletId) {
        return walletDao.getWalletById(walletId).getBalance();
    }

    @Test
    public void balance_followsAddDeleteAndUndo() {
        long w = addWallet(100);
        addTx(w, 50, TransactionType.INCOME);
        long expense = addTx(w, 30, TransactionType.EXPENSE);
        assertEquals(120, balance(w), 0.001);

        transactionDao.setDeleted(expense, true, 1);
        assertEquals(150, balance(w), 0.001);

        // Undo used to subtract a second time; now it just restores the row.
        transactionDao.setDeleted(expense, false, 2);
        assertEquals(120, balance(w), 0.001);
    }

    @Test
    public void deleteWallet_softDeletesItsTransactions() {
        long w = addWallet(0);
        long tx = addTx(w, 10, TransactionType.EXPENSE);

        walletDao.deleteWallet(w, 5);

        assertNotNull(walletDao.getWalletById(w));
        assertTrue(walletDao.getWalletById(w).getWallet().isDeleted());
        try (Cursor c = db.query("SELECT deleted, syncStatus, updatedAt FROM transactions WHERE id = " + tx, null)) {
            c.moveToFirst();
            assertEquals(1, c.getInt(0));
            assertEquals("PENDING", c.getString(1));
            assertEquals(5, c.getLong(2));
        }
    }
}

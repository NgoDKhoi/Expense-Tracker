package com.example.expensetracker.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.expensetracker.core.database.dao.WalletDao;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    private static final String DB = "migration-test";
    private static final String UUID_V4 = "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(), ExpenseDatabase.class);

    @Test
    public void migrate8To9_keepsDataAndBalances() throws Exception {
        try (SupportSQLiteDatabase db = helper.createDatabase(DB, 8)) {
            // Wallet 1 shows 1000 after +500 income and -200 expense, so it started at 700.
            db.execSQL("INSERT INTO wallet_table (id, name, balance, colorHex, iconName) VALUES (1, 'Tiền mặt', 1000, '#FF5722', 'w')");
            db.execSQL("INSERT INTO wallet_table (id, name, balance, colorHex, iconName) VALUES (2, 'Bank', 50, '#000000', 'w')");
            db.execSQL("INSERT INTO transactions (amount, category, timestamp, syncStatus, walletId, type) VALUES (500, 'Salary', 1000, 'PENDING', 1, 'INCOME')");
            db.execSQL("INSERT INTO transactions (amount, category, timestamp, syncStatus, walletId, type) VALUES (200, 'Food', 2000, 'PENDING', 1, 'EXPENSE')");
            db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Food', 'EXPENSE', '#4CAF50', 'restaurant')");
            db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Food', 'EXPENSE', '#111111', 'restaurant')");
            db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Mine', 'EXPENSE', '#222222', 'x')");
        }

        SupportSQLiteDatabase db = helper.runMigrationsAndValidate(DB, 9, true, ExpenseDatabase.MIGRATION_8_9);

        try (Cursor c = db.query("SELECT id, initialBalance, uuid, syncStatus FROM wallet_table ORDER BY id")) {
            c.moveToNext();
            assertEquals(700, c.getDouble(1), 0.001);
            assertEquals(ExpenseDatabase.DEFAULT_WALLET_UUID, c.getString(2));
            assertEquals("PENDING", c.getString(3));
            c.moveToNext();
            assertEquals(50, c.getDouble(1), 0.001);
            assertTrue(c.getString(2).matches(UUID_V4));
        }

        // The shown balance must not change.
        try (Cursor c = db.query(WalletDao.WITH_BALANCE + " ORDER BY w.id")) {
            int col = c.getColumnIndexOrThrow("balance");
            c.moveToNext();
            assertEquals(1000, c.getDouble(col), 0.001);
            c.moveToNext();
            assertEquals(50, c.getDouble(col), 0.001);
        }

        try (Cursor c = db.query("SELECT uuid, deleted, updatedAt FROM transactions ORDER BY id")) {
            Set<String> uuids = new HashSet<>();
            while (c.moveToNext()) {
                assertTrue(c.getString(0).matches(UUID_V4));
                uuids.add(c.getString(0));
                assertEquals(0, c.getInt(1));
            }
            assertEquals(2, uuids.size());
        }

        try (Cursor c = db.query("SELECT uuid FROM categories ORDER BY id")) {
            c.moveToNext();
            assertEquals(ExpenseDatabase.defaultCategoryUuid("EXPENSE", "Food"), c.getString(0));
            c.moveToNext();
            String duplicate = c.getString(0);
            assertTrue(duplicate.matches(UUID_V4));
            assertNotEquals(ExpenseDatabase.defaultCategoryUuid("EXPENSE", "Food"), duplicate);
            c.moveToNext();
            assertTrue(c.getString(0).matches(UUID_V4));
        }
    }
}

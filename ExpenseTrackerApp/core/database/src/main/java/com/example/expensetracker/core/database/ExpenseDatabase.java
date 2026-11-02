package com.example.expensetracker.core.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expensetracker.core.database.converter.Converters;
import com.example.expensetracker.core.database.dao.CategoryDao;
import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.entity.CategoryEntity;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.WalletEntity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Database(entities = {TransactionEntity.class, WalletEntity.class, CategoryEntity.class}, version = 9, exportSchema = true)
@TypeConverters({Converters.class})
public abstract class ExpenseDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();
    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();

    private static volatile ExpenseDatabase INSTANCE;

    /** name, type, colorHex, iconName */
    private static final String[][] DEFAULT_CATEGORIES = {
            {"Food", "EXPENSE", "#4CAF50", "restaurant"},
            {"Transport", "EXPENSE", "#2196F3", "directions_car"},
            {"Utilities", "EXPENSE", "#FF9800", "bolt"},
            {"Shopping", "EXPENSE", "#9C27B0", "shopping_bag"},
            {"Entertainment", "EXPENSE", "#E91E63", "movie"},
            {"Other", "EXPENSE", "#607D8B", "more_horiz"},
            {"Salary", "INCOME", "#4CAF50", "payments"},
            {"Bonus", "INCOME", "#8BC34A", "card_giftcard"},
            {"Investment", "INCOME", "#CDDC39", "trending_up"},
            {"Other", "INCOME", "#607D8B", "more_horiz"},
    };
    private static final String DEFAULT_WALLET_NAME = "Tiền mặt";

    // SQLite expression for a random UUID v4, evaluated per row.
    private static final String RANDOM_UUID = "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-'"
            + " || substr('89ab', 1 + (abs(random()) % 4), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))";

    /** Default rows get the same uuid on every device, so they merge instead of duplicating on sync. */
    static String fixedUuid(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    static String defaultCategoryUuid(String type, String name) {
        return fixedUuid("category:" + type + ":" + name);
    }

    static final String DEFAULT_WALLET_UUID = fixedUuid("wallet:default");

    /** v9 adds sync columns (uuid, updatedAt, deleted) and replaces the stored wallet balance with initialBalance. */
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            long now = System.currentTimeMillis();

            db.execSQL("ALTER TABLE transactions ADD COLUMN uuid TEXT");
            db.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE transactions ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE transactions SET uuid = " + RANDOM_UUID + ", updatedAt = COALESCE(timestamp, " + now + "), syncStatus = 'PENDING'");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_uuid ON transactions (uuid)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_timestamp ON transactions (timestamp)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_walletId ON transactions (walletId)");

            // Keep the shown balance: initialBalance = stored balance - net of the wallet's transactions.
            db.execSQL("CREATE TABLE IF NOT EXISTS wallet_table_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT,"
                    + " `initialBalance` REAL NOT NULL, `colorHex` TEXT, `iconName` TEXT, `uuid` TEXT,"
                    + " `updatedAt` INTEGER NOT NULL DEFAULT 0, `deleted` INTEGER NOT NULL DEFAULT 0, `syncStatus` TEXT)");
            db.execSQL("INSERT INTO wallet_table_new (id, name, initialBalance, colorHex, iconName, uuid, updatedAt, deleted, syncStatus)"
                    + " SELECT w.id, w.name,"
                    + " w.balance - COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)"
                    + " FROM transactions t WHERE t.walletId = w.id), 0),"
                    + " w.colorHex, w.iconName,"
                    + " CASE WHEN w.id = (SELECT MIN(id) FROM wallet_table WHERE name = ?) THEN ? ELSE " + RANDOM_UUID + " END,"
                    + " CASE WHEN w.id = (SELECT MIN(id) FROM wallet_table WHERE name = ?) THEN 0 ELSE ? END, 0, 'PENDING' FROM wallet_table w",
                    new Object[]{DEFAULT_WALLET_NAME, DEFAULT_WALLET_UUID, DEFAULT_WALLET_NAME, now});
            db.execSQL("DROP TABLE wallet_table");
            db.execSQL("ALTER TABLE wallet_table_new RENAME TO wallet_table");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_wallet_table_uuid ON wallet_table (uuid)");

            db.execSQL("ALTER TABLE categories ADD COLUMN uuid TEXT");
            db.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE categories ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE categories ADD COLUMN syncStatus TEXT");
            db.execSQL("UPDATE categories SET updatedAt = ?, syncStatus = 'PENDING'", new Object[]{now});
            // Only the oldest row per default (name, type) gets the fixed uuid, so a user-made duplicate can't break the unique index.
            for (String[] c : DEFAULT_CATEGORIES) {
                db.execSQL("UPDATE categories SET uuid = ?, updatedAt = 0 WHERE id = (SELECT MIN(id) FROM categories WHERE name = ? AND type = ?)",
                        new Object[]{defaultCategoryUuid(c[1], c[0]), c[0], c[1]});
            }
            db.execSQL("UPDATE categories SET uuid = " + RANDOM_UUID + " WHERE uuid IS NULL");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_uuid ON categories (uuid)");
        }
    };

    public static ExpenseDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ExpenseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ExpenseDatabase.class, "expense_database")
                            .addMigrations(MIGRATION_8_9)
                            // Versions before 8 never shipped to users, so wiping them is fine.
                            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    seedDefaults(db);
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    try {
                                        seedDefaults(db);
                                    } catch (Exception ignored) {
                                    }
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Seeds default categories / wallet when the table has no visible rows. REPLACE revives a soft-deleted default.
     * updatedAt = 0: on a new device the account's own copy of these rows (if any) is newer and wins.
     */
    static void seedDefaults(SupportSQLiteDatabase db) {
        if (countLive(db, "categories") == 0) {
            for (String[] c : DEFAULT_CATEGORIES) {
                db.execSQL("INSERT OR REPLACE INTO categories (uuid, name, type, colorHex, iconName, updatedAt, deleted, syncStatus)"
                        + " VALUES (?, ?, ?, ?, ?, 0, 0, 'PENDING')",
                        new Object[]{defaultCategoryUuid(c[1], c[0]), c[0], c[1], c[2], c[3]});
            }
        }
        if (countLive(db, "wallet_table") == 0) {
            db.execSQL("INSERT OR REPLACE INTO wallet_table (uuid, name, initialBalance, colorHex, iconName, updatedAt, deleted, syncStatus)"
                    + " VALUES (?, ?, 0, '#FF5722', 'account_balance_wallet', 0, 0, 'PENDING')",
                    new Object[]{DEFAULT_WALLET_UUID, DEFAULT_WALLET_NAME});
        }
    }

    private static int countLive(SupportSQLiteDatabase db, String table) {
        try (Cursor cursor = db.query("SELECT COUNT(*) FROM " + table + " WHERE deleted = 0")) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }
}

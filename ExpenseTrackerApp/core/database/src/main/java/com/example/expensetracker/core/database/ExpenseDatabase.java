package com.example.expensetracker.core.database;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.expensetracker.core.database.converter.Converters;
import com.example.expensetracker.core.database.dao.CategoryDao;
import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.entity.CategoryEntity;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.WalletEntity;

@Database(entities = {TransactionEntity.class, WalletEntity.class, CategoryEntity.class}, version = 8, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ExpenseDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();
    public abstract WalletDao walletDao();
    public abstract CategoryDao categoryDao();

    private static volatile ExpenseDatabase INSTANCE;

    public static ExpenseDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ExpenseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ExpenseDatabase.class, "expense_database")
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    seedDefaults(db);
                                }

                                @Override
                                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                    super.onOpen(db);
                                    try (Cursor cursor = db.query("SELECT COUNT(*) FROM categories")) {
                                        if (cursor != null && cursor.moveToFirst() && cursor.getInt(0) == 0) {
                                            seedDefaults(db);
                                        }
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

    private static void seedDefaults(SupportSQLiteDatabase db) {
        // Default categories
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Food', 'EXPENSE', '#4CAF50', 'restaurant')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Transport', 'EXPENSE', '#2196F3', 'directions_car')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Utilities', 'EXPENSE', '#FF9800', 'bolt')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Shopping', 'EXPENSE', '#9C27B0', 'shopping_bag')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Entertainment', 'EXPENSE', '#E91E63', 'movie')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Other', 'EXPENSE', '#607D8B', 'more_horiz')");

        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Salary', 'INCOME', '#4CAF50', 'payments')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Bonus', 'INCOME', '#8BC34A', 'card_giftcard')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Investment', 'INCOME', '#CDDC39', 'trending_up')");
        db.execSQL("INSERT INTO categories (name, type, colorHex, iconName) VALUES ('Other', 'INCOME', '#607D8B', 'more_horiz')");

        // Seed default wallet if empty
        try (Cursor cursor = db.query("SELECT COUNT(*) FROM wallet_table")) {
            if (cursor == null || !cursor.moveToFirst() || cursor.getInt(0) == 0) {
                db.execSQL("INSERT INTO wallet_table (name, balance, colorHex, iconName) VALUES ('Tiền mặt', 0, '#FF5722', 'account_balance_wallet')");
            }
        } catch (Exception ignored) {
        }
    }
}

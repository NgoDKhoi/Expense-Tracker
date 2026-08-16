package com.example.expensetracker.core.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.example.expensetracker.core.database.converter.Converters;
import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.WalletEntity;

@Database(entities = {TransactionEntity.class, WalletEntity.class}, version = 6, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ExpenseDatabase extends RoomDatabase {

    public abstract TransactionDao transactionDao();
    public abstract WalletDao walletDao();

    private static volatile ExpenseDatabase INSTANCE;

    public static ExpenseDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ExpenseDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ExpenseDatabase.class, "expense_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}

package com.example.expensetracker.core.database.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.expensetracker.core.database.ExpenseDatabase;
import com.example.expensetracker.core.database.dao.CategoryDao;
import com.example.expensetracker.core.database.dao.TransactionDao;
import com.example.expensetracker.core.database.dao.WalletDao;
import com.example.expensetracker.core.database.entity.CategoryEntity;
import com.example.expensetracker.core.database.entity.PendingTransaction;
import com.example.expensetracker.core.database.entity.SyncStatus;
import com.example.expensetracker.core.database.entity.SyncableEntity;
import com.example.expensetracker.core.database.entity.TransactionEntity;
import com.example.expensetracker.core.database.entity.WalletEntity;
import com.example.expensetracker.core.database.mapper.EntityMapper;
import com.example.expensetracker.core.domain.model.CategoryModel;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.core.domain.model.WalletModel;
import com.example.expensetracker.core.domain.repository.ISyncLocal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** ISyncLocal on Room. The pull cursors live in a small SharedPreferences file. */
@Singleton
public class SyncLocalImpl implements ISyncLocal {
    private static final String TAG = "SyncLocalImpl";
    private static final String PREFS = "sync_state";
    private static final String KEY_LAST_SYNCED_AT = "last_synced_at";
    // Receipt photos are saved here by AddTransactionFragment; they are not backed up yet.
    private static final String PHOTO_DIR = "expense_images";

    private final Context context;

    private final ExpenseDatabase db;
    private final WalletDao walletDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final SharedPreferences prefs;

    @Inject
    public SyncLocalImpl(Context context, ExpenseDatabase db) {
        this.context = context;
        this.db = db;
        this.walletDao = db.walletDao();
        this.categoryDao = db.categoryDao();
        this.transactionDao = db.transactionDao();
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public List<WalletModel> getPendingWallets() {
        List<WalletModel> out = new ArrayList<>();
        for (WalletEntity e : walletDao.getPending()) out.add(EntityMapper.toModel(e));
        return out;
    }

    @Override
    public List<CategoryModel> getPendingCategories() {
        return EntityMapper.toCategoryModelList(categoryDao.getPending());
    }

    @Override
    public List<TransactionModel> getPendingTransactions() {
        List<TransactionModel> out = new ArrayList<>();
        for (PendingTransaction p : transactionDao.getPendingWithWalletUuid()) {
            TransactionModel m = EntityMapper.toModel(p.getTransaction());
            m.setWalletUuid(p.getWalletUuid());
            out.add(m);
        }
        return out;
    }

    @Override
    public void markSynced(Table table, String uuid, long updatedAt) {
        switch (table) {
            case WALLETS: walletDao.markSynced(uuid, updatedAt); break;
            case CATEGORIES: categoryDao.markSynced(uuid, updatedAt); break;
            case TRANSACTIONS: transactionDao.markSynced(uuid, updatedAt); break;
        }
    }

    @Override
    public Long getPendingUpdatedAt(Table table, String uuid) {
        SyncableEntity row;
        switch (table) {
            case WALLETS: row = walletDao.findByUuid(uuid); break;
            case CATEGORIES: row = categoryDao.findByUuid(uuid); break;
            default: row = transactionDao.findByUuid(uuid); break;
        }
        return row != null && row.getSyncStatus() == SyncStatus.PENDING ? row.getUpdatedAt() : null;
    }

    @Override
    public void saveRemoteWallet(WalletModel wallet) {
        WalletEntity e = EntityMapper.toEntity(wallet);
        e.setSyncStatus(SyncStatus.SYNCED);
        WalletEntity existing = walletDao.findByUuid(wallet.getUuid());
        if (existing == null) {
            e.setId(0);
            walletDao.insertWallet(e);
        } else {
            e.setId(existing.getId());
            walletDao.updateWallet(e);
        }
    }

    @Override
    public void saveRemoteCategory(CategoryModel category) {
        CategoryEntity e = EntityMapper.toEntity(category);
        e.setSyncStatus(SyncStatus.SYNCED);
        CategoryEntity existing = categoryDao.findByUuid(category.getUuid());
        if (existing == null) {
            e.setId(0);
            categoryDao.insertCategory(e);
        } else {
            e.setId(existing.getId());
            categoryDao.updateCategory(e);
        }
    }

    @Override
    public void saveRemoteTransaction(TransactionModel transaction) {
        WalletEntity wallet = walletDao.findByUuid(transaction.getWalletUuid());
        if (wallet == null) {
            // Wallets are pulled first, so this only happens for a wallet already deleted and purged here.
            Log.w(TAG, "Skipping transaction " + transaction.getUuid() + ": unknown wallet");
            return;
        }
        TransactionEntity e = EntityMapper.toEntity(transaction);
        e.setWalletId(wallet.getId());
        e.setSyncStatus(SyncStatus.SYNCED);
        TransactionEntity existing = transactionDao.findByUuid(transaction.getUuid());
        if (existing == null) {
            e.setId(0);
            transactionDao.insertExpense(e);
        } else {
            e.setId(existing.getId());
            e.setImageUri(existing.getImageUri()); // photos are not synced yet; keep this phone's copy
            transactionDao.updateExpense(e);
        }
    }

    @Override
    public void purgeSyncedDeletes() {
        db.runInTransaction(() -> {
            transactionDao.purgeSyncedDeletes();
            categoryDao.purgeSyncedDeletes();
            walletDao.purgeSyncedDeletes();
        });
    }

    @Override
    public long getCursor(Table table) {
        return prefs.getLong(table.name(), 0);
    }

    @Override
    public void setCursor(Table table, long syncedAtMillis) {
        prefs.edit().putLong(table.name(), syncedAtMillis).apply();
    }

    @Override
    public int countUnsyncedTransactions() {
        return transactionDao.countUnsynced();
    }

    @Override
    public int countPendingChanges() {
        return walletDao.countPendingChanges() + categoryDao.countPendingChanges() + transactionDao.countPendingChanges();
    }

    @Override
    public long getLastSyncedAt() {
        return prefs.getLong(KEY_LAST_SYNCED_AT, 0);
    }

    @Override
    public void setLastSyncedAt(long millis) {
        prefs.edit().putLong(KEY_LAST_SYNCED_AT, millis).apply();
    }

    @Override
    public void clearAll() {
        db.clearAndSeedDefaults();
        // commit(): the cursors must be gone before the next sync pulls the new account's data.
        prefs.edit().clear().commit();
        File[] photos = new File(context.getFilesDir(), PHOTO_DIR).listFiles();
        if (photos != null) {
            for (File photo : photos) {
                if (!photo.delete()) Log.w(TAG, "Could not delete " + photo.getName());
            }
        }
    }
}

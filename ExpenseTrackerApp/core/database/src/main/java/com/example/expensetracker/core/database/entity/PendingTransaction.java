package com.example.expensetracker.core.database.entity;

import androidx.room.Embedded;

/** A transaction waiting to be pushed, with its wallet's uuid (the server links by uuid). */
public class PendingTransaction {
    @Embedded
    private TransactionEntity transaction;
    private String walletUuid;

    public TransactionEntity getTransaction() { return transaction; }
    public void setTransaction(TransactionEntity transaction) { this.transaction = transaction; }

    public String getWalletUuid() { return walletUuid; }
    public void setWalletUuid(String walletUuid) { this.walletUuid = walletUuid; }
}

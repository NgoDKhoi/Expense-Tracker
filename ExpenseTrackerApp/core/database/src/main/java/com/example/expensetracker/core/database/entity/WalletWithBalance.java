package com.example.expensetracker.core.database.entity;

import androidx.room.Embedded;

/** A wallet row plus its balance computed from transactions. */
public class WalletWithBalance {
    @Embedded
    private WalletEntity wallet;
    private double balance;

    public WalletEntity getWallet() { return wallet; }
    public void setWallet(WalletEntity wallet) { this.wallet = wallet; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

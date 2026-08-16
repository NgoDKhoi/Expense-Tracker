package com.example.expensetracker.core.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallet_table")
public class WalletEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    private double balance;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

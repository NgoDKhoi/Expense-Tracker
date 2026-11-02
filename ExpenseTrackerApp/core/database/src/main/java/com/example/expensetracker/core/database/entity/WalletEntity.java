package com.example.expensetracker.core.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "wallet_table", indices = {@Index(value = "uuid", unique = true)})
public class WalletEntity extends SyncableEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String name;
    // Current balance = initialBalance + income - expense of live transactions (see WalletDao).
    private double initialBalance;
    private String colorHex;
    private String iconName;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}

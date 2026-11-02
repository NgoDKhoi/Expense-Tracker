package com.example.expensetracker.core.domain.model;

public class WalletModel {
    private long id;
    private String uuid;
    private String name;
    private double initialBalance;
    // Computed by the database from transactions; read-only for callers.
    private double balance;
    private String colorHex;
    private String iconName;
    private long updatedAt;
    private boolean deleted;

    public WalletModel() {
    }

    public WalletModel(long id, String name, double initialBalance) {
        this(id, name, initialBalance, "#FF5722", "account_balance_wallet");
    }

    /** For a new wallet: it has no transactions yet, so balance == initialBalance. */
    public WalletModel(long id, String name, double initialBalance, String colorHex, String iconName) {
        this.id = id;
        this.name = name;
        this.initialBalance = initialBalance;
        this.balance = initialBalance;
        this.colorHex = colorHex != null ? colorHex : "#FF5722";
        this.iconName = iconName != null ? iconName : "account_balance_wallet";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getInitialBalance() { return initialBalance; }
    public void setInitialBalance(double initialBalance) { this.initialBalance = initialBalance; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getColorHex() { return colorHex != null ? colorHex : "#FF5722"; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getIconName() { return iconName != null ? iconName : "account_balance_wallet"; }
    public void setIconName(String iconName) { this.iconName = iconName; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
}

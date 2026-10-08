package com.example.expensetracker.core.domain.model;

public class WalletModel {
    private long id;
    private String name;
    private double balance;
    private String colorHex;
    private String iconName;

    public WalletModel() {
    }

    public WalletModel(long id, String name, double balance) {
        this(id, name, balance, "#FF5722", "account_balance_wallet");
    }

    public WalletModel(long id, String name, double balance, String colorHex, String iconName) {
        this.id = id;
        this.name = name;
        this.balance = balance;
        this.colorHex = colorHex != null ? colorHex : "#FF5722";
        this.iconName = iconName != null ? iconName : "account_balance_wallet";
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getColorHex() { return colorHex != null ? colorHex : "#FF5722"; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getIconName() { return iconName != null ? iconName : "account_balance_wallet"; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}

package com.example.expensetracker.core.domain.model;

public class WalletModel {
    private long id;
    private String name;
    private double balance;

    public WalletModel() {
    }

    public WalletModel(long id, String name, double balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

package com.example.expensetracker.core.domain.model;

import java.util.Date;

public class TransactionModel {
    private long id;
    private double amount;
    private String title;
    private String category;
    private String imageUri;
    private Date timestamp;
    private String syncStatus;
    private String note;
    private long walletId;
    private String type;

    public TransactionModel() {
    }

    private TransactionModel(Builder builder) {
        this.id = builder.id;
        this.amount = builder.amount;
        this.title = builder.title;
        this.category = builder.category;
        this.imageUri = builder.imageUri;
        this.timestamp = builder.timestamp;
        this.syncStatus = builder.syncStatus;
        this.note = builder.note;
        this.walletId = builder.walletId;
        this.type = builder.type;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getWalletId() { return walletId; }
    public void setWalletId(long walletId) { this.walletId = walletId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public static class Builder {
        private long id;
        private double amount;
        private String title;
        private String category;
        private String imageUri;
        private Date timestamp;
        private String syncStatus;
        private String note;
        private long walletId;
        private String type;

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder imageUri(String imageUri) {
            this.imageUri = imageUri;
            return this;
        }

        public Builder timestamp(Date timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder syncStatus(String syncStatus) {
            this.syncStatus = syncStatus;
            return this;
        }

        public Builder note(String note) {
            this.note = note;
            return this;
        }

        public Builder walletId(long walletId) {
            this.walletId = walletId;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public TransactionModel build() {
            return new TransactionModel(this);
        }
    }
}

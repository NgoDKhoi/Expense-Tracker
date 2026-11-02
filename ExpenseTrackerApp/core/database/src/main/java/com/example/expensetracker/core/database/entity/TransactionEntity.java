package com.example.expensetracker.core.database.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "transactions", indices = {
        @Index(value = "uuid", unique = true),
        @Index("timestamp"),
        @Index("walletId")})
public class TransactionEntity extends SyncableEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private double amount;
    private String title;
    private String category;
    private String imageUri;
    private Date timestamp;
    private String note;
    private long walletId;
    private TransactionType type = TransactionType.EXPENSE;

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
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public long getWalletId() { return walletId; }
    public void setWalletId(long walletId) { this.walletId = walletId; }
    
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
}

package com.example.expensetracker.core.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String type;
    private String colorHex;
    private String iconName;

    public CategoryEntity() {
    }

    public CategoryEntity(String name, String type, String colorHex, String iconName) {
        this.name = name;
        this.type = type;
        this.colorHex = colorHex;
        this.iconName = iconName;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getIconName() { return iconName; }
    public void setIconName(String iconName) { this.iconName = iconName; }
}

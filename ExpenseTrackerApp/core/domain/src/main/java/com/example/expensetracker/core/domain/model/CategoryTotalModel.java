package com.example.expensetracker.core.domain.model;

public class CategoryTotalModel {
    private String category;
    private double total;

    public CategoryTotalModel() {
    }

    public CategoryTotalModel(String category, double total) {
        this.category = category;
        this.total = total;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
}

package com.example.expensetracker.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<TransactionModel> expenses;
    private Context context;

    public PhotoAdapter(Context context, List<TransactionModel> expenses) {
        this.context = context;
        this.expenses = expenses;
    }

    public void setExpenses(List<TransactionModel> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        TransactionModel expense = expenses.get(position);

        if (expense.getImageUri() != null && !expense.getImageUri().isEmpty()) {
            Glide.with(context)
                    .load(expense.getImageUri())
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.ivPhoto);
        } else {
            Glide.with(context)
                    .load(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivPhoto);
        }

        holder.itemView.setOnClickListener(v -> showDetailsDialog(expense));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    private void showDetailsDialog(TransactionModel expense) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String dateStr = sdf.format(expense.getTimestamp());

        String details = "Title: " + expense.getTitle() + "\n" +
                "Amount: $" + String.format(Locale.getDefault(), "%.2f", expense.getAmount()) + "\n" +
                "Type: " + expense.getType() + "\n" +
                "Category: " + expense.getCategory() + "\n" +
                "Date: " + dateStr + "\n" +
                "Note: " + (expense.getNote() != null ? expense.getNote() : "N/A");

        new AlertDialog.Builder(context)
                .setTitle("Transaction Details")
                .setMessage(details)
                .setPositiveButton("Close", null)
                .show();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
        }
    }
}

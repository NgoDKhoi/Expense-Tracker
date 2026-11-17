package com.example.expensetracker.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;
import com.example.expensetracker.util.CurrencyFormatter;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<TransactionModel> photoTransactions = new ArrayList<>();
    private final Context context;
    private final OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(TransactionModel expense);
    }

    public PhotoAdapter(Context context, List<TransactionModel> allExpenses, OnPhotoClickListener listener) {
        this.context = context;
        this.listener = listener;
        setExpenses(allExpenses);
    }

    public void setExpenses(List<TransactionModel> allExpenses) {
        this.photoTransactions.clear();
        if (allExpenses != null) {
            for (TransactionModel tx : allExpenses) {
                if (tx != null && !tx.isDeleted()) {
                    this.photoTransactions.add(tx);
                }
            }
        }
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
        TransactionModel expense = photoTransactions.get(position);
        int defaultRes = "INCOME".equalsIgnoreCase(expense.getType())
                ? R.drawable.bg_default_receipt_income
                : R.drawable.bg_default_receipt_expense;

        if (expense.getImageUri() != null && !expense.getImageUri().trim().isEmpty()) {
            Glide.with(context)
                    .load(expense.getImageUri())
                    .placeholder(defaultRes)
                    .error(defaultRes)
                    .into(holder.ivPhoto);
        } else {
            Glide.with(context).clear(holder.ivPhoto);
            holder.ivPhoto.setImageResource(defaultRes);
        }

        holder.tvAmount.setText(CurrencyFormatter.formatCompact(context, expense.getAmount()));
        holder.itemView.setOnClickListener(v -> listener.onPhotoClick(expense));
    }

    @Override
    public int getItemCount() {
        return photoTransactions.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvAmount;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            tvAmount = itemView.findViewById(R.id.tv_photo_amount);
        }
    }
}

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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                if (tx.getImageUri() != null && !tx.getImageUri().trim().isEmpty()) {
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

        Glide.with(context)
                .load(expense.getImageUri())
                .placeholder(R.drawable.ic_photo_camera)
                .error(R.drawable.ic_photo_camera)
                .into(holder.ivPhoto);

        holder.tvAmount.setText(formatCompactCurrency(expense.getAmount()));
        holder.itemView.setOnClickListener(v -> listener.onPhotoClick(expense));
    }

    @Override
    public int getItemCount() {
        return photoTransactions.size();
    }

    private String formatCompactCurrency(double amount) {
        Locale locale = Locale.US;
        if (amount >= 1_000_000_000) {
            return String.format(locale, "%.1fB ₫", amount / 1_000_000_000.0);
        } else if (amount >= 1_000_000) {
            return String.format(locale, "%.1fM ₫", amount / 1_000_000.0);
        } else if (amount >= 1_000) {
            return String.format(locale, "%.1fK ₫", amount / 1_000.0);
        } else {
            NumberFormat format = NumberFormat.getNumberInstance(locale);
            format.setMinimumFractionDigits(0);
            return format.format(amount) + " ₫";
        }
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

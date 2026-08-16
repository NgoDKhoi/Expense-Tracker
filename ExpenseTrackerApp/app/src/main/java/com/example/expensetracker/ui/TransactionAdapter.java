package com.example.expensetracker.ui;

import android.annotation.SuppressLint;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionModel> expenses = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());

    @SuppressLint("NotifyDataSetChanged")
    public void setExpenses(List<TransactionModel> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionModel record = expenses.get(position);
        
        holder.tvTitle.setText(record.getTitle() != null ? record.getTitle() : "");
        holder.tvCategory.setText(record.getCategory());
        holder.tvDate.setText(dateFormat.format(record.getTimestamp()));
        holder.tvAmount.setText(currencyFormat.format(record.getAmount()));
        
        Glide.with(holder.itemView.getContext())
             .load(record.getImageUri())
             .placeholder(android.R.drawable.ic_menu_gallery)
             .into(holder.ivThumbnail);
    }

    @Override
    public int getItemCount() {
        return expenses == null ? 0 : expenses.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvCategory;
        TextView tvDate;
        TextView tvAmount;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }
    }
}

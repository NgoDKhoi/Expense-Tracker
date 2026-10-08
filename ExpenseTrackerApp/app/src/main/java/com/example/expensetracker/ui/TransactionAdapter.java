package com.example.expensetracker.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.expensetracker.R;
import com.example.expensetracker.core.domain.model.TransactionModel;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<TransactionModel> expenses = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private OnTransactionClickListener clickListener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionModel transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.clickListener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setExpenses(List<TransactionModel> expenses) {
        this.expenses = expenses != null ? new ArrayList<>(expenses) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public TransactionModel getItemAt(int position) {
        if (position >= 0 && position < expenses.size()) {
            return expenses.get(position);
        }
        return null;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < expenses.size()) {
            expenses.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void restoreItem(TransactionModel item, int position) {
        if (position >= 0 && position <= expenses.size()) {
            expenses.add(position, item);
            notifyItemInserted(position);
        }
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
        Context context = holder.itemView.getContext();

        String title = record.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = record.getCategory() != null ? record.getCategory() : context.getString(R.string.cat_other);
        }
        holder.tvTitle.setText(title);
        holder.tvCategory.setText(record.getCategory());
        holder.tvDate.setText(dateFormat.format(record.getTimestamp()));

        // Number format: US Locale with compact thousands separator
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        String formattedAmount = format.format(record.getAmount()) + " ₫";

        boolean isIncome = "INCOME".equalsIgnoreCase(record.getType());
        if (isIncome) {
            holder.tvAmount.setText("+ " + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.income_green));
        } else {
            holder.tvAmount.setText("- " + formattedAmount);
            holder.tvAmount.setTextColor(ContextCompat.getColor(context, R.color.expense_orange));
        }

        // Thumbnail binding with fallback
        if (record.getImageUri() != null && !record.getImageUri().trim().isEmpty()) {
            holder.ivThumbnail.setImageTintList(null);
            Glide.with(context)
                    .load(record.getImageUri())
                    .placeholder(R.drawable.ic_photo_camera)
                    .error(R.drawable.ic_photo_camera)
                    .into(holder.ivThumbnail);
        } else {
            Glide.with(context).clear(holder.ivThumbnail);
            holder.ivThumbnail.setImageResource(R.drawable.ic_photo_camera);
            holder.ivThumbnail.setImageTintList(ContextCompat.getColorStateList(context, R.color.text_hint));
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTransactionClick(record);
            }
        });
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

package com.example.expensetracker.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.GamificationViewModel.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter displaying conversation bubbles between User and Piggy AI.
 */
public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    private final List<ChatMessage> messages = new ArrayList<>();

    public void submitMessages(List<ChatMessage> items) {
        messages.clear();
        if (items != null) {
            messages.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (msg.isUser) {
            holder.llPiggyRow.setVisibility(View.GONE);
            holder.llUserRow.setVisibility(View.VISIBLE);
            holder.tvUserText.setText(msg.text);
        } else {
            holder.llUserRow.setVisibility(View.GONE);
            holder.llPiggyRow.setVisibility(View.VISIBLE);
            holder.tvPiggyText.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout llPiggyRow;
        final TextView tvPiggyText;
        final LinearLayout llUserRow;
        final TextView tvUserText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            llPiggyRow = itemView.findViewById(R.id.ll_piggy_message_row);
            tvPiggyText = itemView.findViewById(R.id.tv_piggy_message_text);
            llUserRow = itemView.findViewById(R.id.ll_user_message_row);
            tvUserText = itemView.findViewById(R.id.tv_user_message_text);
        }
    }
}

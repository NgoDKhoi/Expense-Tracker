package com.example.expensetracker.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.GamificationViewModel.EvolutionStage;
import com.example.expensetracker.viewmodel.GamificationViewModel.LeaderboardEntry;
import com.example.expensetracker.viewmodel.GamificationViewModel.RankingMetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter rendering the Piggy Saver Leaderboard rows.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardEntry> entries = new ArrayList<>();
    private RankingMetric metric = RankingMetric.STREAK;

    public void submitData(List<LeaderboardEntry> items, RankingMetric currentMetric) {
        entries.clear();
        if (items != null) {
            entries.addAll(items);
        }
        if (currentMetric != null) {
            metric = currentMetric;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry item = entries.get(position);
        Context context = holder.itemView.getContext();

        holder.itemView.setBackgroundResource(item.isCurrentUser
                ? R.drawable.bg_leaderboard_current_user
                : R.drawable.bg_leaderboard_item);

        holder.tvRank.setText(formatRank(item.rank));
        holder.tvInitial.setText(initialOf(item.displayName));

        String nameText = item.isCurrentUser
                ? item.displayName + context.getString(R.string.leaderboard_you_suffix)
                : item.displayName;
        holder.tvName.setText(nameText);

        holder.tvLevel.setText(stageLabel(context, item.stage) + " · " + item.username);

        int score = metric == RankingMetric.STREAK ? item.streakDays : item.totalSafeDays;
        holder.tvScore.setText(context.getString(R.string.leaderboard_score_days, score));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    private static String formatRank(int rank) {
        switch (rank) {
            case 1:
                return "🥇";
            case 2:
                return "🥈";
            case 3:
                return "🥉";
            default:
                return "#" + rank;
        }
    }

    private static String initialOf(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "?";
        }
        String trimmed = displayName.trim();
        return trimmed.substring(0, trimmed.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT);
    }

    public static String stageLabel(Context context, EvolutionStage stage) {
        if (stage == null) {
            return context.getString(R.string.piggy_level_baby);
        }
        switch (stage) {
            case STUDENT:
                return context.getString(R.string.piggy_level_student);
            case ADULT:
                return context.getString(R.string.piggy_level_adult);
            case ROYAL:
                return context.getString(R.string.piggy_level_royal);
            case BABY:
            default:
                return context.getString(R.string.piggy_level_baby);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvRank;
        final TextView tvInitial;
        final TextView tvName;
        final TextView tvLevel;
        final TextView tvScore;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvInitial = itemView.findViewById(R.id.tv_user_initial);
            tvName = itemView.findViewById(R.id.tv_user_name);
            tvLevel = itemView.findViewById(R.id.tv_user_piggy_level);
            tvScore = itemView.findViewById(R.id.tv_user_score);
        }
    }
}

package com.example.expensetracker.ui;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.ui.adapter.LeaderboardAdapter;
import com.example.expensetracker.viewmodel.GamificationViewModel;
import com.example.expensetracker.viewmodel.GamificationViewModel.LeaderboardEntry;
import com.example.expensetracker.viewmodel.GamificationViewModel.PiggyUiState;
import com.example.expensetracker.viewmodel.GamificationViewModel.RankingMetric;
import com.example.expensetracker.viewmodel.GamificationViewModel.RankingScope;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import javax.inject.Inject;

/**
 * Saving Leaderboard tab inside DashboardFragment, sharing GamificationViewModel with AiAssistantFragment.
 */
public class LeaderboardFragment extends Fragment {

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private GamificationViewModel viewModel;
    private LeaderboardAdapter leaderboardAdapter;

    private TextView tvUserRankSummary;
    private TextView tvUserStageSummary;
    private TextView tvUserStreakSummary;
    private TextView tvUserTotalDaysSummary;
    private MaterialButton btnMetricStreak;
    private MaterialButton btnMetricTotalDays;
    private ChipGroup chipGroupScope;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(GamificationViewModel.class);

        bindViews(view);
        setupRecyclerView(view);
        setupInteractions(view);
        observeViewModel();

        requireActivity().getSupportFragmentManager().setFragmentResultListener(
                AuthFragment.RESULT_AUTH_CHANGED, getViewLifecycleOwner(),
                (key, result) -> {
                    viewModel.refreshUserIdentity();
                    viewModel.syncLeaderboardWithCloud();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshUserIdentity();
            viewModel.syncLeaderboardWithCloud();
        }
    }

    private void bindViews(@NonNull View view) {
        tvUserRankSummary = view.findViewById(R.id.tv_user_rank_summary);
        tvUserStageSummary = view.findViewById(R.id.tv_user_stage_summary);
        tvUserStreakSummary = view.findViewById(R.id.tv_user_streak_summary);
        tvUserTotalDaysSummary = view.findViewById(R.id.tv_user_total_days_summary);
        btnMetricStreak = view.findViewById(R.id.btn_metric_streak);
        btnMetricTotalDays = view.findViewById(R.id.btn_metric_total_days);
        chipGroupScope = view.findViewById(R.id.chip_group_scope);
    }

    private void setupRecyclerView(@NonNull View view) {
        RecyclerView rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setNestedScrollingEnabled(false);
        leaderboardAdapter = new LeaderboardAdapter();
        rvLeaderboard.setAdapter(leaderboardAdapter);
    }

    private void setupInteractions(@NonNull View view) {
        btnMetricStreak.setOnClickListener(v -> {
            btnMetricStreak.setChecked(true);
            btnMetricTotalDays.setChecked(false);
            viewModel.setRankingMetric(RankingMetric.STREAK);
        });

        btnMetricTotalDays.setOnClickListener(v -> {
            btnMetricStreak.setChecked(false);
            btnMetricTotalDays.setChecked(true);
            viewModel.setRankingMetric(RankingMetric.TOTAL_SAFE_DAYS);
        });

        chipGroupScope.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_scope_friends)) {
                viewModel.setRankingScope(RankingScope.FRIENDS);
            } else if (checkedIds.contains(R.id.chip_scope_global)) {
                viewModel.setRankingScope(RankingScope.GLOBAL);
            }
        });

        View btnAddFriend = view.findViewById(R.id.btn_add_friend);
        if (btnAddFriend != null) {
            btnAddFriend.setOnClickListener(v -> showAddFriendDialog());
        }
    }

    private void showAddFriendDialog() {
        int pad = Math.round(20 * requireContext().getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint(R.string.dialog_add_friend_hint);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_add_friend_title)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String handle = input.getText() != null ? input.getText().toString().trim() : "";
                    if (viewModel.addFriendByUsername(handle)) {
                        String normalized = handle.startsWith("@") ? handle : "@" + handle;
                        viewModel.setRankingScope(RankingScope.FRIENDS);
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.msg_friend_added, normalized),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .show();
    }

    private void observeViewModel() {
        viewModel.getPiggyState().observe(getViewLifecycleOwner(), this::renderUserStandingSummary);

        viewModel.getLeaderboardEntries().observe(getViewLifecycleOwner(), entries -> {
            leaderboardAdapter.submitData(entries, viewModel.getCurrentMetric().getValue());
            updateRankSummary(entries);
        });

        viewModel.getCurrentMetric().observe(getViewLifecycleOwner(), metric -> {
            boolean isStreak = metric == RankingMetric.STREAK;
            btnMetricStreak.setChecked(isStreak);
            btnMetricTotalDays.setChecked(!isStreak);
            leaderboardAdapter.submitData(viewModel.getLeaderboardEntries().getValue(), metric);
        });

        viewModel.getCurrentScope().observe(getViewLifecycleOwner(), scope -> {
            if (scope == RankingScope.FRIENDS) {
                chipGroupScope.check(R.id.chip_scope_friends);
            } else {
                chipGroupScope.check(R.id.chip_scope_global);
            }
        });
    }

    private void renderUserStandingSummary(@Nullable PiggyUiState state) {
        if (state == null) {
            return;
        }
        tvUserStageSummary.setText(LeaderboardAdapter.stageLabel(requireContext(), state.stage));
        tvUserStreakSummary.setText(getString(R.string.piggy_streak_badge, state.streakDays));
        tvUserTotalDaysSummary.setText(getString(R.string.piggy_total_days_badge, state.totalSafeDays));
    }

    private void updateRankSummary(@Nullable List<LeaderboardEntry> entries) {
        if (entries == null) {
            return;
        }
        for (LeaderboardEntry entry : entries) {
            if (entry.isCurrentUser) {
                tvUserRankSummary.setText(getString(R.string.leaderboard_your_rank_format, entry.rank));
                return;
            }
        }
    }
}

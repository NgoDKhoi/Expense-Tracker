package com.example.expensetracker.ui;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.ExpenseTrackerApplication;
import com.example.expensetracker.R;
import com.example.expensetracker.ui.adapter.ChatMessageAdapter;
import com.example.expensetracker.ui.adapter.LeaderboardAdapter;
import com.example.expensetracker.viewmodel.GamificationViewModel;
import com.example.expensetracker.viewmodel.GamificationViewModel.Mood;
import com.example.expensetracker.viewmodel.GamificationViewModel.PiggyUiState;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.NumberFormat;
import java.util.Locale;

import javax.inject.Inject;

/**
 * Dedicated full-screen overlay for the Virtual Piggy Pet & Gemini AI Chatbot.
 */
public class AiAssistantFragment extends Fragment {

    public static final String TAG = "AiAssistantFragment";

    @Inject
    ViewModelProvider.Factory viewModelFactory;

    private GamificationViewModel viewModel;
    private ChatMessageAdapter chatAdapter;

    private NestedScrollView nsvPiggyScroll;
    private TextView tvStreakCount;
    private TextView tvTotalSafeDays;
    private TextView tvPiggySpeech;
    private FrameLayout flPiggyAvatarContainer;
    private ImageView ivPiggyAvatar;
    private TextView tvPiggyLevelBadge;
    private TextView tvPiggyMoodStatus;
    private TextView tvDailyBudgetAmount;
    private ProgressBar pbDailyBudget;
    private TextView tvTypingIndicator;
    private EditText etChatInput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        ((ExpenseTrackerApplication) requireActivity().getApplication()).getAppComponent().inject(this);
        viewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(GamificationViewModel.class);

        bindViews(view);
        setupChatRecyclerView(view);
        setupInteractions(view);
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshUserIdentity();
        }
    }

    private void bindViews(@NonNull View view) {
        nsvPiggyScroll = view.findViewById(R.id.nsv_piggy_scroll);
        tvStreakCount = view.findViewById(R.id.tv_streak_count);
        tvTotalSafeDays = view.findViewById(R.id.tv_total_safe_days);
        tvPiggySpeech = view.findViewById(R.id.tv_piggy_speech);
        flPiggyAvatarContainer = view.findViewById(R.id.fl_piggy_avatar_container);
        ivPiggyAvatar = view.findViewById(R.id.iv_piggy_avatar);
        tvPiggyLevelBadge = view.findViewById(R.id.tv_piggy_level_badge);
        tvPiggyMoodStatus = view.findViewById(R.id.tv_piggy_mood_status);
        tvDailyBudgetAmount = view.findViewById(R.id.tv_daily_budget_amount);
        pbDailyBudget = view.findViewById(R.id.pb_daily_budget);
        tvTypingIndicator = view.findViewById(R.id.tv_piggy_typing_indicator);
        etChatInput = view.findViewById(R.id.et_chat_input);
    }

    private void setupChatRecyclerView(@NonNull View view) {
        RecyclerView rvChat = view.findViewById(R.id.rv_piggy_chat);
        rvChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvChat.setNestedScrollingEnabled(false);
        chatAdapter = new ChatMessageAdapter();
        rvChat.setAdapter(chatAdapter);
    }

    private void setupInteractions(@NonNull View view) {
        view.findViewById(R.id.btn_piggy_back).setOnClickListener(v ->
                getParentFragmentManager().popBackStack());

        flPiggyAvatarContainer.setOnClickListener(v -> {
            flPiggyAvatarContainer.animate().cancel();
            flPiggyAvatarContainer.animate()
                    .scaleX(0.86f)
                    .scaleY(0.86f)
                    .setDuration(90)
                    .withEndAction(() -> flPiggyAvatarContainer.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .setDuration(220)
                            .start())
                    .start();

            tvPiggySpeech.animate().cancel();
            tvPiggySpeech.setAlpha(0.4f);
            tvPiggySpeech.animate().alpha(1.0f).setDuration(180).start();

            viewModel.onPiggyTapped();
        });

        view.findViewById(R.id.btn_edit_daily_limit).setOnClickListener(v -> showEditDailyLimitDialog());

        view.findViewById(R.id.chip_prompt_today).setOnClickListener(v ->
                viewModel.sendChatMessage(getString(R.string.chip_prompt_today)));
        view.findViewById(R.id.chip_prompt_top_category).setOnClickListener(v ->
                viewModel.sendChatMessage(getString(R.string.chip_prompt_top_category)));
        view.findViewById(R.id.chip_prompt_streak_tips).setOnClickListener(v ->
                viewModel.sendChatMessage(getString(R.string.chip_prompt_streak_tips)));
        view.findViewById(R.id.chip_prompt_wallet_health).setOnClickListener(v ->
                viewModel.sendChatMessage(getString(R.string.chip_prompt_wallet_health)));

        view.findViewById(R.id.btn_send_chat).setOnClickListener(v -> submitChatInput());

        etChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitChatInput();
                return true;
            }
            return false;
        });
    }

    private void submitChatInput() {
        String text = etChatInput.getText() != null ? etChatInput.getText().toString().trim() : "";
        if (text.isEmpty()) {
            return;
        }
        etChatInput.setText("");
        viewModel.sendChatMessage(text);
    }

    private void showEditDailyLimitDialog() {
        PiggyUiState current = viewModel.getPiggyState().getValue();
        double currentLimit = current != null ? current.dailyLimit : GamificationViewModel.DEFAULT_DAILY_BUDGET_LIMIT;

        int pad = Math.round(20 * requireContext().getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);

        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.dialog_daily_limit_hint);
        input.setText(String.valueOf(Math.round(currentLimit)));
        input.setSelection(input.getText().length());
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_daily_limit_title)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String raw = input.getText() != null ? input.getText().toString().trim() : "";
                    try {
                        double parsed = Double.parseDouble(raw);
                        if (parsed > 0) {
                            viewModel.setDailyBudgetLimit(parsed);
                            Toast.makeText(requireContext(), R.string.msg_daily_limit_updated, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    Toast.makeText(requireContext(), R.string.msg_invalid_amount, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void observeViewModel() {
        viewModel.getPiggyState().observe(getViewLifecycleOwner(), this::renderPiggyState);

        viewModel.getChatMessages().observe(getViewLifecycleOwner(), messages -> {
            chatAdapter.submitMessages(messages);
            if (messages != null && messages.size() > 1 && nsvPiggyScroll != null) {
                nsvPiggyScroll.post(() -> nsvPiggyScroll.fullScroll(View.FOCUS_DOWN));
            }
        });

        viewModel.getIsAiTyping().observe(getViewLifecycleOwner(), typing -> {
            if (tvTypingIndicator != null) {
                tvTypingIndicator.setVisibility(Boolean.TRUE.equals(typing) ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void renderPiggyState(@Nullable PiggyUiState state) {
        if (state == null) {
            return;
        }
        tvStreakCount.setText(getString(R.string.piggy_streak_badge, state.streakDays));
        tvTotalSafeDays.setText(getString(R.string.piggy_total_days_badge, state.totalSafeDays));
        tvPiggySpeech.setText(state.speechBubbleText);
        tvPiggyLevelBadge.setText(LeaderboardAdapter.stageLabel(requireContext(), state.stage));

        int moodColorRes = moodColorRes(state.mood);
        int moodColor = ContextCompat.getColor(requireContext(), moodColorRes);

        ivPiggyAvatar.setImageResource(moodDrawableRes(state.mood));
        tvPiggyMoodStatus.setText(moodTextRes(state.mood));
        tvPiggyMoodStatus.setTextColor(moodColor);

        ViewCompat.setBackgroundTintList(
                flPiggyAvatarContainer,
                ColorStateList.valueOf(ColorUtils.setAlphaComponent(moodColor, 60))
        );
        pbDailyBudget.setProgress(state.budgetProgressPercent);
        pbDailyBudget.setProgressTintList(ColorStateList.valueOf(moodColor));

        String spentFormatted = formatFullCurrency(state.dailySpent);
        String limitFormatted = formatFullCurrency(state.dailyLimit);
        tvDailyBudgetAmount.setText(spentFormatted + " / " + limitFormatted);
    }

    private static int moodDrawableRes(Mood mood) {
        switch (mood) {
            case WORRIED:
                return R.drawable.ic_piggy_worried;
            case SAD:
                return R.drawable.ic_piggy_sad;
            case HAPPY:
            default:
                return R.drawable.ic_piggy_happy;
        }
    }

    private static int moodTextRes(Mood mood) {
        switch (mood) {
            case WORRIED:
                return R.string.piggy_mood_worried;
            case SAD:
                return R.string.piggy_mood_sad;
            case HAPPY:
            default:
                return R.string.piggy_mood_happy;
        }
    }

    private static int moodColorRes(Mood mood) {
        switch (mood) {
            case WORRIED:
                return R.color.category_utilities;
            case SAD:
                return R.color.expense_orange;
            case HAPPY:
            default:
                return R.color.income_green;
        }
    }

    private static String formatFullCurrency(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(0);
        return format.format(amount) + " ₫";
    }
}

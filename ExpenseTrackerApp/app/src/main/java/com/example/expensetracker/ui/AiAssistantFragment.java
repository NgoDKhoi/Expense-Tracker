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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
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
        int gap = Math.round(10 * requireContext().getResources().getDisplayMetrics().density);

        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(pad, gap, pad, 0);
        scrollView.addView(layout);

        EditText inputLimit = new EditText(requireContext());
        inputLimit.setInputType(InputType.TYPE_CLASS_NUMBER);
        inputLimit.setHint(R.string.dialog_daily_limit_hint);
        inputLimit.setText(String.valueOf(Math.round(currentLimit)));
        inputLimit.setSelection(inputLimit.getText().length());
        layout.addView(inputLimit);

        TextView tvWizardTitle = new TextView(requireContext());
        tvWizardTitle.setText(R.string.piggy_wizard_section_title);
        tvWizardTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.expense_orange));
        tvWizardTitle.setTextSize(14f);
        tvWizardTitle.setPadding(0, gap * 2, 0, gap / 2);
        layout.addView(tvWizardTitle);

        EditText etIncome = new EditText(requireContext());
        etIncome.setInputType(InputType.TYPE_CLASS_NUMBER);
        etIncome.setHint(R.string.piggy_wizard_income_hint);
        double monthIncome = viewModel.getCurrentMonthIncome();
        if (monthIncome > 0) {
            etIncome.setText(String.valueOf(Math.round(monthIncome)));
        }
        layout.addView(etIncome);

        EditText etFixed = new EditText(requireContext());
        etFixed.setInputType(InputType.TYPE_CLASS_NUMBER);
        etFixed.setHint(R.string.piggy_wizard_fixed_hint);
        layout.addView(etFixed);

        final int[] selectedSavingPct = {20};
        TextView tvSavingLabel = new TextView(requireContext());
        tvSavingLabel.setText(getString(R.string.piggy_wizard_saving_label, selectedSavingPct[0]));
        tvSavingLabel.setTextSize(13f);
        tvSavingLabel.setPadding(0, gap, 0, gap / 2);
        layout.addView(tvSavingLabel);

        ChipGroup savingChips = new ChipGroup(requireContext());
        savingChips.setSingleSelection(true);
        savingChips.setSelectionRequired(true);
        int[] pctOptions = {10, 20, 30};
        for (int pct : pctOptions) {
            Chip chip = new Chip(requireContext());
            chip.setCheckable(true);
            chip.setText(pct + "%");
            if (pct == 20) {
                chip.setChecked(true);
            }
            chip.setOnClickListener(v -> {
                selectedSavingPct[0] = pct;
                tvSavingLabel.setText(getString(R.string.piggy_wizard_saving_label, pct));
            });
            savingChips.addView(chip);
        }
        layout.addView(savingChips);

        MaterialButton btnCalculate = new MaterialButton(requireContext());
        btnCalculate.setText(R.string.piggy_wizard_calc_btn);
        btnCalculate.setOnClickListener(v -> {
            double income = parseDoubleOrZero(etIncome);
            if (income <= 0) {
                etIncome.setError(getString(R.string.msg_invalid_amount));
                return;
            }
            double fixed = parseDoubleOrZero(etFixed);
            double suggested = GamificationViewModel.suggestDailyBudget(income, fixed, selectedSavingPct[0]);
            inputLimit.setText(String.valueOf(Math.round(suggested)));
            inputLimit.setSelection(inputLimit.getText().length());
            Toast.makeText(
                    requireContext(),
                    getString(R.string.piggy_wizard_suggested_msg, formatFullCurrency(suggested)),
                    Toast.LENGTH_SHORT
            ).show();
        });
        layout.addView(btnCalculate);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_daily_limit_title)
                .setView(scrollView)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String raw = inputLimit.getText() != null ? inputLimit.getText().toString().trim() : "";
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

    private static double parseDoubleOrZero(EditText editText) {
        String raw = editText.getText() != null ? editText.getText().toString().trim() : "";
        if (raw.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 0.0;
        }
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

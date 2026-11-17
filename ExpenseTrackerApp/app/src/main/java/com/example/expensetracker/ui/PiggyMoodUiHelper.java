package com.example.expensetracker.ui;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.example.expensetracker.R;
import com.example.expensetracker.viewmodel.GamificationViewModel.Mood;

/**
 * Maps {@link Mood} states to consistent Piggy drawables, theme colors, and status labels
 * shared between the floating Chat Head and {@link AiAssistantFragment}.
 */
public final class PiggyMoodUiHelper {

    private PiggyMoodUiHelper() {
    }

    @DrawableRes
    public static int moodDrawableRes(@Nullable Mood mood) {
        if (mood == null) {
            return R.drawable.ic_piggy_happy;
        }
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

    @StringRes
    public static int moodTextRes(@Nullable Mood mood) {
        if (mood == null) {
            return R.string.piggy_mood_happy;
        }
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

    @ColorRes
    public static int moodColorRes(@Nullable Mood mood) {
        if (mood == null) {
            return R.color.income_green;
        }
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
}

package com.example.expensetracker.ui;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

public class DepthPageTransformer implements ViewPager2.PageTransformer {
    private static final float MIN_SCALE = 0.85f;

    @Override
    public void transformPage(@NonNull View view, float position) {
        int pageWidth = view.getWidth();

        if (position <= -1f) {
            view.setVisibility(View.INVISIBLE);
            view.setAlpha(0f);
            view.setTranslationX(0f);
        } else if (position <= 0f) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f);
            view.setTranslationX(0f);
            view.setScaleX(1f);
            view.setScaleY(1f);
        } else if (position < 1f) {
            view.setVisibility(View.VISIBLE);
            view.setAlpha(1f - position);
            view.setTranslationX(pageWidth * -position);

            float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
        } else {
            view.setVisibility(View.INVISIBLE);
            view.setAlpha(0f);
            view.setTranslationX(0f);
        }
    }
}

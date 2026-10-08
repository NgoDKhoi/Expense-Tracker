package com.example.expensetracker.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class OnboardingDialogFragment extends DialogFragment {

    public static final String PREFS_NAME = "expense_tracker_prefs";
    public static final String KEY_FIRST_LAUNCH = "is_first_launch";

    public static OnboardingDialogFragment newInstance() {
        return new OnboardingDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewPager2 vpOnboarding = view.findViewById(R.id.vp_onboarding);
        MaterialButton btnSkip = view.findViewById(R.id.btn_onboarding_skip);
        MaterialButton btnNext = view.findViewById(R.id.btn_onboarding_next);

        List<SlideItem> slides = new ArrayList<>();
        slides.add(new SlideItem(R.drawable.ic_photo_camera, R.string.onboarding_title_1, R.string.onboarding_desc_1));
        slides.add(new SlideItem(R.drawable.ic_flip_camera, R.string.onboarding_title_2, R.string.onboarding_desc_2));
        slides.add(new SlideItem(R.drawable.ic_wallet, R.string.onboarding_title_3, R.string.onboarding_desc_3));

        vpOnboarding.setAdapter(new OnboardingAdapter(slides));

        vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == slides.size() - 1) {
                    btnNext.setText(R.string.action_get_started);
                    btnSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText(R.string.action_next);
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });

        btnSkip.setOnClickListener(v -> completeOnboarding());

        btnNext.setOnClickListener(v -> {
            int current = vpOnboarding.getCurrentItem();
            if (current < slides.size() - 1) {
                vpOnboarding.setCurrentItem(current + 1, true);
            } else {
                completeOnboarding();
            }
        });
    }

    private void completeOnboarding() {
        if (getContext() != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
        dismiss();
    }

    private static class SlideItem {
        final int iconRes;
        final int titleRes;
        final int descRes;

        SlideItem(int iconRes, int titleRes, int descRes) {
            this.iconRes = iconRes;
            this.titleRes = titleRes;
            this.descRes = descRes;
        }
    }

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder> {

        private final List<SlideItem> slides;

        OnboardingAdapter(List<SlideItem> slides) {
            this.slides = slides;
        }

        @NonNull
        @Override
        public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
            SlideItem item = slides.get(position);
            holder.ivIcon.setImageResource(item.iconRes);
            holder.tvTitle.setText(item.titleRes);
            holder.tvDesc.setText(item.descRes);
        }

        @Override
        public int getItemCount() {
            return slides.size();
        }

        static class SlideViewHolder extends RecyclerView.ViewHolder {
            ImageView ivIcon;
            TextView tvTitle;
            TextView tvDesc;

            SlideViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_slide_icon);
                tvTitle = itemView.findViewById(R.id.tv_slide_title);
                tvDesc = itemView.findViewById(R.id.tv_slide_desc);
            }
        }
    }
}

package com.example.expensetracker;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.expensetracker.ui.AiAssistantFragment;
import com.example.expensetracker.ui.AuthFragment;
import com.example.expensetracker.ui.CameraHostFragment;
import com.example.expensetracker.ui.DashboardFragment;
import com.example.expensetracker.ui.DepthPageTransformer;
import com.example.expensetracker.ui.OnboardingDialogFragment;
import com.example.expensetracker.ui.PhotosFragment;
import com.example.expensetracker.ui.PiggyMoodUiHelper;
import com.example.expensetracker.ui.widget.FloatingChatHeadHelper;
import com.example.expensetracker.viewmodel.GamificationViewModel;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_ONBOARDING = "ONBOARDING";

    @Inject
    ViewModelProvider.Factory viewModelFactory;
    @Inject
    SharedPreferences sharedPreferences;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ViewPager2 viewPager;
    private boolean openedReceiptFromPhotos = false;

    private View aiChatHead;
    private View flChatHeadBody;
    private ImageView ivChatHeadPiggy;
    private View vChatHeadStatusDot;
    private FloatingChatHeadHelper chatHeadHelper;
    private GamificationViewModel gamificationViewModel;
    private boolean isOnboardingShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((ExpenseTrackerApplication) getApplication()).getAppComponent().inject(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View rootContainer = findViewById(R.id.main);
        setupFloatingAiChatHead(rootContainer);

        ViewCompat.setOnApplyWindowInsetsListener(rootContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            if (chatHeadHelper != null) {
                chatHeadHelper.updateBottomInset(systemBars.bottom);
            }
            return insets;
        });

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (!isGranted) {
                Toast.makeText(this, "Camera permission is required to add receipts", Toast.LENGTH_SHORT).show();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        viewPager = findViewById(R.id.main_view_pager);
        MainPagerAdapter pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageTransformer(new DepthPageTransformer());

        // Start on Camera page (Index 1)
        viewPager.post(() -> viewPager.setCurrentItem(1, false));
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_IDLE && viewPager.getCurrentItem() != 1) {
                    openedReceiptFromPhotos = false;
                    CameraHostFragment cameraHost = findCameraHostFragment();
                    if (cameraHost != null) {
                        cameraHost.resetToCameraImmediate();
                    }
                }
            }
        });

        // Check Onboarding
        if (getSupportFragmentManager().findFragmentByTag(TAG_ONBOARDING) != null) {
            isOnboardingShowing = true;
        } else if (sharedPreferences.getBoolean(OnboardingDialogFragment.KEY_FIRST_LAUNCH, true)) {
            isOnboardingShowing = true;
            OnboardingDialogFragment.newInstance().show(getSupportFragmentManager(), TAG_ONBOARDING);
        }
        updateChatHeadVisibility();

        handleAuthDeepLink(getIntent());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }
                if (viewPager != null) {
                    if (viewPager.getCurrentItem() == 1) {
                        CameraHostFragment cameraHost = findCameraHostFragment();
                        if (cameraHost != null && !cameraHost.isOnCameraPage()) {
                            if (openedReceiptFromPhotos) {
                                openedReceiptFromPhotos = false;
                                viewPager.setCurrentItem(2, true);
                            } else {
                                cameraHost.returnToCamera();
                            }
                            return;
                        }
                    } else {
                        viewPager.setCurrentItem(1, true);
                        return;
                    }
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    public void openReceiptInCameraHost(long transactionId) {
        openedReceiptFromPhotos = true;
        CameraHostFragment cameraHost = findCameraHostFragment();
        if (cameraHost != null) {
            cameraHost.scrollToTransaction(transactionId);
        }
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }

    private void setupFloatingAiChatHead(@NonNull View rootContainer) {
        aiChatHead = findViewById(R.id.ai_chat_head);
        flChatHeadBody = findViewById(R.id.fl_chat_head_body);
        ivChatHeadPiggy = findViewById(R.id.iv_chat_head_piggy);
        vChatHeadStatusDot = findViewById(R.id.v_chat_head_status_dot);

        if (aiChatHead == null) {
            return;
        }

        chatHeadHelper = new FloatingChatHeadHelper(aiChatHead, rootContainer, sharedPreferences);
        chatHeadHelper.attach();
        aiChatHead.setOnClickListener(v -> openAiAssistantOverlay());

        gamificationViewModel = new ViewModelProvider(this, viewModelFactory).get(GamificationViewModel.class);
        gamificationViewModel.getPiggyState().observe(this, this::renderChatHeadMood);

        getSupportFragmentManager().addOnBackStackChangedListener(this::updateChatHeadVisibility);
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentStarted(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        if (f instanceof OnboardingDialogFragment) {
                            isOnboardingShowing = true;
                            updateChatHeadVisibility();
                        }
                    }

                    @Override
                    public void onFragmentDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        if (f instanceof OnboardingDialogFragment) {
                            isOnboardingShowing = false;
                            updateChatHeadVisibility();
                        }
                    }
                },
                false
        );
    }

    private void renderChatHeadMood(@Nullable GamificationViewModel.PiggyUiState state) {
        if (state == null || ivChatHeadPiggy == null || flChatHeadBody == null || vChatHeadStatusDot == null) {
            return;
        }
        int moodColor = ContextCompat.getColor(this, PiggyMoodUiHelper.moodColorRes(state.mood));
        ivChatHeadPiggy.setImageResource(PiggyMoodUiHelper.moodDrawableRes(state.mood));

        Drawable bodyBg = flChatHeadBody.getBackground();
        if (bodyBg != null) {
            Drawable mutatedBody = bodyBg.mutate();
            if (mutatedBody instanceof GradientDrawable) {
                GradientDrawable gd = (GradientDrawable) mutatedBody;
                int strokePx = Math.round(2.5f * getResources().getDisplayMetrics().density);
                int tintedFill = ColorUtils.compositeColors(
                        ColorUtils.setAlphaComponent(moodColor, 46),
                        0xEB181818
                );
                gd.setColor(tintedFill);
                gd.setStroke(strokePx, moodColor);
            }
        }

        Drawable dotBg = vChatHeadStatusDot.getBackground();
        if (dotBg != null) {
            Drawable mutatedDot = dotBg.mutate();
            if (mutatedDot instanceof GradientDrawable) {
                ((GradientDrawable) mutatedDot).setColor(moodColor);
            }
        }
    }

    private void openAiAssistantOverlay() {
        if (getSupportFragmentManager().findFragmentByTag(AiAssistantFragment.TAG) != null) {
            return;
        }
        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(
                        android.R.anim.fade_in,
                        android.R.anim.fade_out,
                        android.R.anim.fade_in,
                        android.R.anim.fade_out
                )
                .add(android.R.id.content, new AiAssistantFragment(), AiAssistantFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    private void updateChatHeadVisibility() {
        if (aiChatHead == null) {
            return;
        }
        boolean shouldShow = FloatingChatHeadHelper.shouldShowChatHead(
                getSupportFragmentManager().getBackStackEntryCount(),
                isOnboardingShowing
        );
        boolean wasVisible = aiChatHead.getVisibility() == View.VISIBLE;
        if (shouldShow) {
            if (!wasVisible) {
                aiChatHead.setAlpha(0f);
                aiChatHead.setVisibility(View.VISIBLE);
                aiChatHead.animate().alpha(1f).setDuration(150L).start();
            }
        } else {
            aiChatHead.animate().cancel();
            aiChatHead.setVisibility(View.GONE);
        }
    }

    public boolean handleReceiptTopBack() {
        if (openedReceiptFromPhotos) {
            openedReceiptFromPhotos = false;
            if (viewPager != null) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        }
        return false;
    }

    public void clearOpenedReceiptFromPhotos() {
        openedReceiptFromPhotos = false;
    }

    @Nullable
    private CameraHostFragment findCameraHostFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof CameraHostFragment && fragment.isAdded()) {
                return (CameraHostFragment) fragment;
            }
        }
        return null;
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthDeepLink(intent);
    }

    private void handleAuthDeepLink(@Nullable Intent intent) {
        if (intent == null) return;
        Uri data = intent.getData();
        if (data == null || !"expensetracker".equals(data.getScheme()) || !"auth-callback".equals(data.getHost())) {
            return;
        }
        String callbackUri = data.toString();
        intent.setData(null);

        Fragment existing = getSupportFragmentManager().findFragmentByTag(AuthFragment.TAG);
        if (existing instanceof AuthFragment) {
            ((AuthFragment) existing).handleOAuthCallback(callbackUri);
        } else {
            AuthFragment.newInstanceWithCallback(callbackUri).show(getSupportFragmentManager(), AuthFragment.TAG);
        }
    }

    private static class MainPagerAdapter extends FragmentStateAdapter {

        public MainPagerAdapter(@NonNull AppCompatActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new DashboardFragment();
                case 1:
                    return new CameraHostFragment();
                case 2:
                    return new PhotosFragment();
                default:
                    return new CameraHostFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}

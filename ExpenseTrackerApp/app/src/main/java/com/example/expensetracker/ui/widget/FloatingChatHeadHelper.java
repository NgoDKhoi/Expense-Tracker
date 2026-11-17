package com.example.expensetracker.ui.widget;

import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Enables Messenger Chat Head-style free dragging, edge snapping, WindowInsets-safe clamping,
 * and SharedPreferences position persistence for a floating bubble view inside a parent container.
 */
public class FloatingChatHeadHelper implements View.OnTouchListener {

    public static final String KEY_SNAP_RIGHT = "ai_chat_head_snap_right";
    public static final String KEY_Y_FRACTION = "ai_chat_head_y_fraction";
    public static final boolean DEFAULT_SNAP_RIGHT = true;
    public static final float DEFAULT_Y_FRACTION = 0.72f;

    private static final float EDGE_MARGIN_DP = 12f;
    private static final float PRESS_SCALE = 0.92f;
    private static final long PRESS_ANIM_MS = 80L;
    private static final long SNAP_ANIM_MS = 260L;
    private static final float SNAP_OVERSHOOT = 1.05f;

    private final View targetView;
    private final View parentContainer;
    @Nullable
    private final SharedPreferences prefs;
    private final float marginPx;
    private final int touchSlop;

    private int bottomInsetPx = 0;
    private boolean hasPositionedOnce = false;
    private int lastParentWidth = -1;
    private int lastParentHeight = -1;
    private int lastViewWidth = -1;
    private int lastViewHeight = -1;

    private float downRawX;
    private float downRawY;
    private float startViewX;
    private float startViewY;
    private boolean isDragging;

    public FloatingChatHeadHelper(@NonNull View targetView,
                                  @NonNull View parentContainer,
                                  @Nullable SharedPreferences prefs) {
        this.targetView = targetView;
        this.parentContainer = parentContainer;
        this.prefs = prefs;
        float density = targetView.getResources().getDisplayMetrics().density;
        this.marginPx = EDGE_MARGIN_DP * density;
        this.touchSlop = ViewConfiguration.get(targetView.getContext()).getScaledTouchSlop();
    }

    /**
     * Attaches touch and layout listeners so the bubble positions cleanly before the first draw
     * and stays inside safe bounds on orientation or system-bar changes.
     */
    public void attach() {
        targetView.setOnTouchListener(this);
        View.OnLayoutChangeListener layoutListener = (v, l, t, r, b, ol, ot, or, ob) -> {
            if (isDragging) {
                return;
            }
            int parentW = parentContainer.getWidth();
            int parentH = parentContainer.getHeight();
            int viewW = targetView.getWidth();
            int viewH = targetView.getHeight();
            if (parentW <= 0 || parentH <= 0 || viewW <= 0 || viewH <= 0) {
                return;
            }
            boolean sizeChanged = parentW != lastParentWidth
                    || parentH != lastParentHeight
                    || viewW != lastViewWidth
                    || viewH != lastViewHeight;
            if (!hasPositionedOnce || sizeChanged) {
                lastParentWidth = parentW;
                lastParentHeight = parentH;
                lastViewWidth = viewW;
                lastViewHeight = viewH;
                applySavedPosition(false);
            }
        };
        parentContainer.addOnLayoutChangeListener(layoutListener);
        targetView.addOnLayoutChangeListener(layoutListener);
    }

    /**
     * Updates the bottom system-bar inset and re-clamps the bubble if not actively dragged.
     */
    public void updateBottomInset(int bottomInsetPx) {
        int normalized = Math.max(0, bottomInsetPx);
        if (this.bottomInsetPx == normalized) {
            return;
        }
        this.bottomInsetPx = normalized;
        if (!isDragging && parentContainer.getWidth() > 0 && targetView.getWidth() > 0) {
            applySavedPosition(false);
        }
    }

    /**
     * Applies the persisted edge & vertical ratio immediately or smoothly.
     */
    public void applySavedPosition(boolean animate) {
        int parentW = parentContainer.getWidth();
        int parentH = parentContainer.getHeight();
        int viewW = targetView.getWidth();
        int viewH = targetView.getHeight();
        if (parentW <= 0 || parentH <= 0 || viewW <= 0 || viewH <= 0) {
            return;
        }

        boolean snapRight = prefs == null || prefs.getBoolean(KEY_SNAP_RIGHT, DEFAULT_SNAP_RIGHT);
        float yFraction = prefs != null ? prefs.getFloat(KEY_Y_FRACTION, DEFAULT_Y_FRACTION) : DEFAULT_Y_FRACTION;

        float minY = computeMinY(parentContainer.getPaddingTop(), marginPx);
        float maxY = computeMaxY(parentH, bottomInsetPx, viewH, marginPx, minY);
        float targetX = computeSnapX(
                snapRight,
                marginPx,
                viewW,
                parentW,
                parentContainer.getPaddingLeft(),
                parentContainer.getPaddingRight()
        );
        float targetY = resolveYFromFraction(yFraction, minY, maxY);

        targetView.animate().cancel();
        if (animate && hasPositionedOnce) {
            targetView.animate()
                    .x(targetX)
                    .y(targetY)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(SNAP_ANIM_MS)
                    .setInterpolator(new OvershootInterpolator(SNAP_OVERSHOOT))
                    .start();
        } else {
            targetView.setX(targetX);
            targetView.setY(targetY);
        }
        hasPositionedOnce = true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int parentW = parentContainer.getWidth();
        int parentH = parentContainer.getHeight();
        int viewW = v.getWidth();
        int viewH = v.getHeight();

        float minX = parentContainer.getPaddingLeft() + marginPx;
        float maxX = Math.max(minX, parentW - parentContainer.getPaddingRight() - viewW - marginPx);
        float minY = computeMinY(parentContainer.getPaddingTop(), marginPx);
        float maxY = computeMaxY(parentH, bottomInsetPx, viewH, marginPx, minY);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                v.animate().cancel();
                v.setAlpha(1f);
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                startViewX = v.getX();
                startViewY = v.getY();
                isDragging = false;
                disallowParentIntercept(v, true);
                v.animate()
                        .scaleX(PRESS_SCALE)
                        .scaleY(PRESS_SCALE)
                        .setDuration(PRESS_ANIM_MS)
                        .start();
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!isDragging && (dx * dx + dy * dy) > (touchSlop * touchSlop)) {
                    isDragging = true;
                }
                if (isDragging) {
                    v.setX(clamp(startViewX + dx, minX, maxX));
                    v.setY(clamp(startViewY + dy, minY, maxY));
                }
                return true;

            case MotionEvent.ACTION_UP:
                disallowParentIntercept(v, false);
                if (!isDragging) {
                    v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(PRESS_ANIM_MS)
                            .start();
                    v.performClick();
                    return true;
                }
                isDragging = false;
                snapAndPersist(v, minY, maxY);
                return true;

            case MotionEvent.ACTION_CANCEL:
                disallowParentIntercept(v, false);
                if (isDragging) {
                    isDragging = false;
                    snapAndPersist(v, minY, maxY);
                } else {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(PRESS_ANIM_MS).start();
                }
                return true;

            default:
                return false;
        }
    }

    private void snapAndPersist(@NonNull View v, float minY, float maxY) {
        int parentW = parentContainer.getWidth();
        int viewW = v.getWidth();
        boolean snapRight = shouldSnapRight(
                v.getX(),
                viewW,
                parentW,
                parentContainer.getPaddingLeft(),
                parentContainer.getPaddingRight()
        );
        float targetX = computeSnapX(
                snapRight,
                marginPx,
                viewW,
                parentW,
                parentContainer.getPaddingLeft(),
                parentContainer.getPaddingRight()
        );
        float targetY = clamp(v.getY(), minY, maxY);
        float yFraction = computeYFraction(targetY, minY, maxY);

        if (prefs != null) {
            prefs.edit()
                    .putBoolean(KEY_SNAP_RIGHT, snapRight)
                    .putFloat(KEY_Y_FRACTION, yFraction)
                    .apply();
        }

        hasPositionedOnce = true;
        v.animate()
                .x(targetX)
                .y(targetY)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(SNAP_ANIM_MS)
                .setInterpolator(new OvershootInterpolator(SNAP_OVERSHOOT))
                .start();
    }

    private static void disallowParentIntercept(@NonNull View v, boolean disallow) {
        ViewParent parent = v.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    public static float clamp(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public static boolean shouldSnapRight(float viewX,
                                          float viewWidth,
                                          float parentWidth,
                                          float leftPadding,
                                          float rightPadding) {
        float viewCenterX = viewX + viewWidth * 0.5f;
        float safeWidth = Math.max(0f, parentWidth - leftPadding - rightPadding);
        float midX = leftPadding + safeWidth * 0.5f;
        return viewCenterX >= midX;
    }

    public static float computeSnapX(boolean snapRight,
                                     float marginPx,
                                     float viewWidth,
                                     float parentWidth,
                                     float leftPadding,
                                     float rightPadding) {
        float minX = leftPadding + marginPx;
        float maxX = parentWidth - rightPadding - viewWidth - marginPx;
        if (maxX < minX) {
            return minX;
        }
        return snapRight ? maxX : minX;
    }

    public static float computeMinY(float topPadding, float marginPx) {
        return topPadding + marginPx;
    }

    public static float computeMaxY(float parentHeight,
                                    float bottomInsetPx,
                                    float viewHeight,
                                    float marginPx,
                                    float minY) {
        float maxY = parentHeight - bottomInsetPx - viewHeight - marginPx;
        return Math.max(minY, maxY);
    }

    public static float computeYFraction(float viewY, float minY, float maxY) {
        float range = maxY - minY;
        if (range <= 0f) {
            return 0f;
        }
        return clamp((viewY - minY) / range, 0f, 1f);
    }

    public static float resolveYFromFraction(float yFraction, float minY, float maxY) {
        float clampedFraction = clamp(yFraction, 0f, 1f);
        return minY + clampedFraction * Math.max(0f, maxY - minY);
    }

    /**
     * Shows the global chat head only when no full-screen overlay fragment or onboarding dialog is active.
     */
    public static boolean shouldShowChatHead(int backStackEntryCount, boolean hasOnboardingDialog) {
        return backStackEntryCount == 0 && !hasOnboardingDialog;
    }
}

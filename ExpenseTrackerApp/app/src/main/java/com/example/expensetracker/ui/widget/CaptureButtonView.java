package com.example.expensetracker.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.drawable.Drawable;

/**
 * Custom Camera Capture Button with outer ring, gap, and inner filled circle.
 * Features touch feedback with smooth scale-down on press and spring overshoot on release.
 * Supports an optional center icon (e.g. checkmark for Save action).
 */
public class CaptureButtonView extends View {

    private static final int OUTER_RING_COLOR = Color.parseColor("#FF5722");
    private static final int INNER_CIRCLE_COLOR = Color.parseColor("#FFFFFF");

    private final Paint outerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float strokeWidthPx;
    private float innerRadiusPx;
    private float outerRadiusPx;

    private float currentScale = 1.0f;
    private float currentOuterAlpha = 1.0f;

    private Drawable centerIcon;
    private int centerIconTint = Color.parseColor("#FF5722");

    private ValueAnimator scaleAnimator;
    private boolean isTouchInside = false;

    public CaptureButtonView(Context context) {
        super(context);
        init();
    }

    public CaptureButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CaptureButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);

        strokeWidthPx = dpToPx(6);
        // Inner circle diameter = 60dp -> radius = 30dp
        innerRadiusPx = dpToPx(30);

        outerRingPaint.setStyle(Paint.Style.STROKE);
        outerRingPaint.setStrokeWidth(strokeWidthPx);
        outerRingPaint.setColor(OUTER_RING_COLOR);

        innerCirclePaint.setStyle(Paint.Style.FILL);
        innerCirclePaint.setColor(INNER_CIRCLE_COLOR);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) dpToPx(80);
        int width = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        int size = Math.min(width, height);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float radius = Math.min(w, h) / 2.0f;
        // Stroke is centered on the stroke line, so subtract half the stroke width
        // Outer diameter 80dp -> outer edge = radius, center of stroke = radius - strokeWidth / 2
        outerRadiusPx = radius - (strokeWidthPx / 2.0f);
    }

    public void setCenterIcon(@Nullable Drawable icon) {
        setCenterIcon(icon, Color.parseColor("#FF5722"));
    }

    public void setCenterIcon(@Nullable Drawable icon, int tintColor) {
        this.centerIcon = icon;
        this.centerIconTint = tintColor;
        if (this.centerIcon != null) {
            this.centerIcon = this.centerIcon.mutate();
            this.centerIcon.setTint(this.centerIconTint);
        }
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;

        canvas.save();
        canvas.scale(currentScale, currentScale, cx, cy);

        // 1. Draw outer ring with dynamic alpha
        int originalAlpha = Color.alpha(OUTER_RING_COLOR);
        outerRingPaint.setAlpha((int) (originalAlpha * currentOuterAlpha));
        canvas.drawCircle(cx, cy, outerRadiusPx, outerRingPaint);

        // 2. Draw inner circle
        canvas.drawCircle(cx, cy, innerRadiusPx, innerCirclePaint);

        // 3. Draw center icon if set
        if (centerIcon != null) {
            float iconSizePx = dpToPx(32);
            int left = (int) (cx - iconSizePx / 2.0f);
            int top = (int) (cy - iconSizePx / 2.0f);
            int right = (int) (cx + iconSizePx / 2.0f);
            int bottom = (int) (cy + iconSizePx / 2.0f);
            centerIcon.setBounds(left, top, right, bottom);
            centerIcon.draw(canvas);
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchInside = true;
                animatePressDown();
                return true;

            case MotionEvent.ACTION_MOVE:
                boolean inside = isPointInside(event.getX(), event.getY());
                if (isTouchInside && !inside) {
                    isTouchInside = false;
                    animateRelease(false);
                } else if (!isTouchInside && inside) {
                    isTouchInside = true;
                    animatePressDown();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (isTouchInside) {
                    isTouchInside = false;
                    animateRelease(true);
                    performClick();
                } else {
                    animateRelease(false);
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                isTouchInside = false;
                animateRelease(false);
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private boolean isPointInside(float x, float y) {
        return x >= 0 && x <= getWidth() && y >= 0 && y <= getHeight();
    }

    private void animatePressDown() {
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }

        final float startScale = currentScale;
        final float targetScale = 0.85f;
        final float startAlpha = currentOuterAlpha;
        final float targetAlpha = 0.8f;

        scaleAnimator = ValueAnimator.ofFloat(0f, 1f);
        scaleAnimator.setDuration(120);
        scaleAnimator.setInterpolator(new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            currentScale = startScale + (targetScale - startScale) * fraction;
            currentOuterAlpha = startAlpha + (targetAlpha - startAlpha) * fraction;
            invalidate();
        });
        scaleAnimator.start();
    }

    private void animateRelease(boolean spring) {
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }

        final float startScale = currentScale;
        final float targetScale = 1.0f;
        final float startAlpha = currentOuterAlpha;
        final float targetAlpha = 1.0f;

        scaleAnimator = ValueAnimator.ofFloat(0f, 1f);
        scaleAnimator.setDuration(spring ? 180 : 150);
        scaleAnimator.setInterpolator(spring ? new OvershootInterpolator(1.5f) : new DecelerateInterpolator());
        scaleAnimator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            currentScale = startScale + (targetScale - startScale) * fraction;
            currentOuterAlpha = startAlpha + (targetAlpha - startAlpha) * fraction;
            invalidate();
        });
        scaleAnimator.start();
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            scaleAnimator.cancel();
        }
    }
}

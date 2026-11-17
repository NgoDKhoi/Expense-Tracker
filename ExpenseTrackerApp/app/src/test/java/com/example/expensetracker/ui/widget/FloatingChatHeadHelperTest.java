package com.example.expensetracker.ui.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FloatingChatHeadHelperTest {

    private static final float DELTA = 0.001f;

    @Test
    public void clamp_boundsValuesWithinRangeAndHandlesInvertedBounds() {
        assertEquals(12f, FloatingChatHeadHelper.clamp(-5f, 12f, 500f), DELTA);
        assertEquals(250f, FloatingChatHeadHelper.clamp(250f, 12f, 500f), DELTA);
        assertEquals(500f, FloatingChatHeadHelper.clamp(750f, 12f, 500f), DELTA);
        assertEquals(100f, FloatingChatHeadHelper.clamp(50f, 100f, 40f), DELTA);
    }

    @Test
    public void shouldSnapRight_determinesNearestHorizontalEdgeIncludingPadding() {
        // Parent width 1080, view width 180, no horizontal padding -> midpoint = 540, center = x + 90
        assertFalse(FloatingChatHeadHelper.shouldSnapRight(100f, 180f, 1080f, 0f, 0f));
        assertTrue(FloatingChatHeadHelper.shouldSnapRight(450f, 180f, 1080f, 0f, 0f));
        assertTrue(FloatingChatHeadHelper.shouldSnapRight(600f, 180f, 1080f, 0f, 0f));

        // Asymmetric left notch padding (100px) shifts safe center to 100 + (1080 - 100) / 2 = 590
        assertFalse(FloatingChatHeadHelper.shouldSnapRight(460f, 180f, 1080f, 100f, 0f));
        assertTrue(FloatingChatHeadHelper.shouldSnapRight(510f, 180f, 1080f, 100f, 0f));
    }

    @Test
    public void computeSnapX_respectsEdgeMarginsAndPadding() {
        float leftSnap = FloatingChatHeadHelper.computeSnapX(false, 24f, 120f, 1080f, 16f, 32f);
        assertEquals(40f, leftSnap, DELTA);

        float rightSnap = FloatingChatHeadHelper.computeSnapX(true, 24f, 120f, 1080f, 16f, 32f);
        assertEquals(1080f - 32f - 120f - 24f, rightSnap, DELTA);
    }

    @Test
    public void verticalFraction_roundTripsAndClampsSafelyAcrossInsets() {
        float minY = FloatingChatHeadHelper.computeMinY(80f, 24f); // 104
        float maxY = FloatingChatHeadHelper.computeMaxY(2400f, 96f, 160f, 24f, minY); // 2120

        assertEquals(104f, minY, DELTA);
        assertEquals(2120f, maxY, DELTA);

        float midFraction = FloatingChatHeadHelper.computeYFraction(1112f, minY, maxY);
        assertEquals(0.5f, midFraction, DELTA);
        assertEquals(1112f, FloatingChatHeadHelper.resolveYFromFraction(midFraction, minY, maxY), DELTA);

        assertEquals(minY, FloatingChatHeadHelper.resolveYFromFraction(-0.5f, minY, maxY), DELTA);
        assertEquals(maxY, FloatingChatHeadHelper.resolveYFromFraction(1.5f, minY, maxY), DELTA);
        assertEquals(0f, FloatingChatHeadHelper.computeYFraction(200f, 300f, 200f), DELTA);
    }

    @Test
    public void shouldShowChatHead_hidesWhenBackStackOrOnboardingActive() {
        assertTrue(FloatingChatHeadHelper.shouldShowChatHead(0, false));
        assertFalse(FloatingChatHeadHelper.shouldShowChatHead(1, false));
        assertFalse(FloatingChatHeadHelper.shouldShowChatHead(0, true));
        assertFalse(FloatingChatHeadHelper.shouldShowChatHead(2, true));
    }
}

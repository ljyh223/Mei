package com.ljyh.mei.ui.component.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetCalculationsTest {
    @Test
    fun stationarySheetUsesMidpointsWithAnchorOffsets() {
        assertEquals(
            BottomSheetTarget.Collapsed,
            resolveFlingTarget(
                value = 400.dp,
                velocity = 0f,
                dismissedBound = 20.dp,
                collapsedBound = 100.dp,
                expandedBound = 900.dp,
                canDismiss = true,
            ),
        )
        assertEquals(
            BottomSheetTarget.Expanded,
            resolveFlingTarget(
                value = 600.dp,
                velocity = 0f,
                dismissedBound = 20.dp,
                collapsedBound = 100.dp,
                expandedBound = 900.dp,
                canDismiss = true,
            ),
        )
    }

    @Test
    fun hiddenMorphHasNoBottomMargin() {
        val layout = resolveMorphLayout(
            maxWidth = 1_000.dp,
            expandedHeight = 800.dp,
            spec = BottomSheetMorphSpec(collapsedHorizontalMargin = 20.dp),
            progress = 0f,
            revealProgress = 0f,
        )

        assertEquals(960.dp, layout.width)
        assertEquals(0.dp, layout.effectiveBottomMargin)
    }

    @Test
    fun downwardFlingOnlyDismissesWhenAllowed() {
        val dismissible = resolveFlingTarget(
            value = 50.dp,
            velocity = -300f,
            dismissedBound = 0.dp,
            collapsedBound = 100.dp,
            expandedBound = 900.dp,
            canDismiss = true,
        )
        val persistent = resolveFlingTarget(
            value = 50.dp,
            velocity = -300f,
            dismissedBound = 0.dp,
            collapsedBound = 100.dp,
            expandedBound = 900.dp,
            canDismiss = false,
        )

        assertEquals(BottomSheetTarget.Dismissed, dismissible)
        assertEquals(BottomSheetTarget.Collapsed, persistent)
    }
}

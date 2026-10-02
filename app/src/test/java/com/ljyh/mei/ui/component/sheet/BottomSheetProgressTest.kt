package com.ljyh.mei.ui.component.sheet

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.EmptyCoroutineContext

class BottomSheetProgressTest {
    @Test
    fun progressIsClampedAcrossAnchorSegment() {
        assertEquals(0f, normalizedProgress((-8).dp, 0.dp, 52.dp))
        assertEquals(0f, normalizedProgress(0.dp, 0.dp, 52.dp))
        assertEquals(0.5f, normalizedProgress(26.dp, 0.dp, 52.dp))
        assertEquals(1f, normalizedProgress(52.dp, 0.dp, 52.dp))
        assertEquals(1f, normalizedProgress(80.dp, 0.dp, 52.dp))
    }

    @Test
    fun zeroLengthSegmentHasStableEndpoints() {
        assertEquals(0f, normalizedProgress(0.dp, 8.dp, 8.dp))
        assertEquals(1f, normalizedProgress(8.dp, 8.dp, 8.dp))
    }

    @Test
    fun stateProgressUsesUpdatedBounds() {
        val density = Density(1f)
        val initialAnchors = DraggableAnchors {
            BottomSheetValue.Dismissed at 0f
            BottomSheetValue.Collapsed at 64f
            BottomSheetValue.Expanded at 914f
        }
        val draggableState = AnchoredDraggableState(
            initialValue = BottomSheetValue.Collapsed,
            anchors = initialAnchors,
        )
        val state = BottomSheetState(
            anchoredDraggableState = draggableState,
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            onAnchorChanged = {},
            density = density,
            dismissedBound = 0.dp,
            collapsedBound = 64.dp,
            expandedBound = 914.dp,
        )
        state.updateAnchors(
            density = density,
            dismissedBound = 0.dp,
            collapsedBound = 88.dp,
            expandedBound = 914.dp,
            allowDismissGesture = true,
        )

        assertEquals(88.dp, state.value)
        assertEquals(0f, state.progress)
    }

    @Test
    fun collapsedPlayerOmitsTheDismissedGestureAnchor() {
        assertFalse(
            shouldIncludeDismissedAnchor(
                allowDismissGesture = false,
                isDismissed = false,
                isTargetDismissed = false,
            ),
        )
    }

    @Test
    fun programmaticDismissKeepsTheDismissedAnchorAvailable() {
        assertTrue(
            shouldIncludeDismissedAnchor(
                allowDismissGesture = false,
                isDismissed = false,
                isTargetDismissed = true,
            ),
        )
        assertTrue(
            shouldIncludeDismissedAnchor(
                allowDismissGesture = false,
                isDismissed = true,
                isTargetDismissed = false,
            ),
        )
    }
}

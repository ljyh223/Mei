package com.ljyh.mei.ui.component.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

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
}

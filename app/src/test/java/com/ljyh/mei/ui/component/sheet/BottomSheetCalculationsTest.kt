package com.ljyh.mei.ui.component.sheet

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BottomSheetCalculationsTest {
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
}

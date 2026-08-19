package com.ljyh.mei.ui.component.sheet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

internal class BottomSheetNestedScrollConnection(
    private val state: BottomSheetState,
) : NestedScrollConnection {
    private var isTopReached = false

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (state.isExpanded && available.y < 0) {
            isTopReached = false
        }

        return if (isTopReached && available.y < 0 && source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(available.y)
            available
        } else {
            Offset.Zero
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!isTopReached) {
            isTopReached = consumed.y == 0f && available.y > 0
        }

        return if (isTopReached && source == NestedScrollSource.UserInput) {
            state.dispatchRawDelta(available.y)
            available
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        return if (isTopReached) {
            state.performFling(-available.y, null)
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        isTopReached = false
        return Velocity.Zero
    }
}

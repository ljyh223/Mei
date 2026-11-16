package com.ljyh.mei.ui.component.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo

internal val LocalPhoneLandscape = staticCompositionLocalOf { false }

/** Space for the floating phone navigation inside the system-safe area. */
internal val LocalPageStartInset = staticCompositionLocalOf { 0.dp }

/** Horizontal safe area passed to the content when the page itself draws edge to edge. */
internal val LocalPageHorizontalSafePadding = staticCompositionLocalOf { PaddingValues(0.dp) }

@Composable
internal fun meiPageHorizontalPadding(gutter: Dp = meiPageGutter()): PaddingValues {
    val safe = LocalPageHorizontalSafePadding.current
    val direction = LocalLayoutDirection.current
    return PaddingValues(
        start = safe.calculateStartPadding(direction) + LocalPageStartInset.current + gutter,
        end = safe.calculateEndPadding(direction) + gutter,
    )
}

/** Stable content insets shared by page bodies and floating chrome. */
object MeiPageLayout {
    val PhoneGutter = 20.dp
    val TabletGutter = 28.dp
    val ToolbarTouchSize = 48.dp
    val ToolbarLensSize = 40.dp
    val ToolbarIconSize = 22.dp
}

@Composable
fun meiPageGutter(): Dp = when (LocalAppWindowLayout.current) {
    AppWindowLayout.Wide -> MeiPageLayout.TabletGutter
    AppWindowLayout.Compact -> MeiPageLayout.PhoneGutter
    null -> if (rememberDeviceInfo().isTablet) MeiPageLayout.TabletGutter else MeiPageLayout.PhoneGutter
}

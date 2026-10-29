package com.ljyh.mei.ui.component.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.utils.rememberDeviceInfo

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

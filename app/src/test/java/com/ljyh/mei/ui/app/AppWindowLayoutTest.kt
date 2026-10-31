package com.ljyh.mei.ui.app

import com.ljyh.mei.ui.component.layout.AppWindowLayout
import com.ljyh.mei.ui.component.layout.appWindowLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppWindowLayoutTest {
    @Test fun compactAndWideShellsUseTheAvailableWidthWithoutAnOrientationOrDeviceFlag() {
        for (width in listOf(0f, 320f, 480f, 599f, Float.NaN))
            assertEquals(AppWindowLayout.Compact, appWindowLayout(width))
        for (width in listOf(600f, 720f, 840f, 1366f))
            assertEquals(AppWindowLayout.Wide, appWindowLayout(width))
    }
    @Test fun sideSafeAreasCanKeepTheSameWindowInTheCompactLayout() {
        assertEquals(AppWindowLayout.Wide, appWindowLayout(620f))
        assertEquals(AppWindowLayout.Compact, appWindowLayout(620f - 32f - 16f))
    }
}

package com.ljyh.mei.playback.lyrics

internal data class DesktopLyricsWindowGeometry(
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
)

/** Full-display coordinates, shared by initial placement, dragging and resizing. */
internal fun desktopLyricsWindowGeometry(
    screenWidth: Int,
    screenHeight: Int,
    desiredWidth: Int,
    desiredHeight: Int,
    x: Int,
    y: Int,
): DesktopLyricsWindowGeometry {
    val availableWidth = screenWidth.coerceAtLeast(1)
    val availableHeight = screenHeight.coerceAtLeast(1)
    val width = desiredWidth.coerceIn(1, availableWidth)
    val height = desiredHeight.coerceIn(1, availableHeight)
    return DesktopLyricsWindowGeometry(
        width = width,
        height = height,
        x = x.coerceIn(0, availableWidth - width),
        y = y.coerceIn(0, availableHeight - height),
    )
}

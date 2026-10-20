package com.ljyh.mei.ui.component.player.overlay

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/** A sheet-to-sheet transition keeps the same modal container mounted. */
internal class PlayerSheetNavigation {
    private val active = mutableStateOf<OverlayState>(OverlayState.None)
    val current: State<OverlayState> = active
    private val history = mutableStateOf<List<OverlayState>>(emptyList())
    private val returning = mutableStateOf(false)

    val canGoBack: Boolean get() = history.value.isNotEmpty()
    val isReturning: Boolean get() = returning.value

    fun show(next: OverlayState) {
        val previous = active.value
        if (previous == next) return
        if (previous != OverlayState.None) history.value += previous
        returning.value = false
        active.value = next
    }

    fun goBack() {
        val previous = history.value.lastOrNull() ?: return dismiss()
        history.value = history.value.dropLast(1)
        returning.value = true
        active.value = previous
    }

    fun dismiss() {
        history.value = emptyList()
        returning.value = false
        active.value = OverlayState.None
    }
}

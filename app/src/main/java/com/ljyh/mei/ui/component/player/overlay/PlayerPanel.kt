package com.ljyh.mei.ui.component.player.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/** In-player content intent, independent of a particular wide or compact arrangement. */
enum class PlayerPanel { Lyrics, Queue, Comments }

@Composable
internal fun rememberPlayerPanelState(): MutableState<PlayerPanel> =
    rememberSaveable { mutableStateOf(PlayerPanel.Lyrics) }

/** Clicking the selected toolbar destination returns to the default lyric panel. */
internal fun nextPlayerPanel(current: PlayerPanel, target: PlayerPanel, toggle: Boolean = true): PlayerPanel =
    if (toggle && current == target) PlayerPanel.Lyrics else target

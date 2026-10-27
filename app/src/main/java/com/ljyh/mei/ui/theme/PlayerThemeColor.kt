package com.ljyh.mei.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.playback.PlayerConnection
import kotlinx.coroutines.flow.collectLatest

@Composable
fun rememberPlayerThemeColor(
    context: Context,
    playerConnection: PlayerConnection?,
    dynamicTheme: Boolean,
    colorRepository: ColorRepository,
): Color {
    var themeColor by remember { mutableStateOf(Color.Black) }

    LaunchedEffect(playerConnection, dynamicTheme, colorRepository) {
        if (!dynamicTheme) return@LaunchedEffect
        val connection = playerConnection ?: return@LaunchedEffect

        connection.service.currentMediaMetadata.collectLatest { song ->
            if (song == null) return@collectLatest
            themeColor = colorRepository.getColorOrExtract(context, song.coverUrl)
        }
    }

    return themeColor
}

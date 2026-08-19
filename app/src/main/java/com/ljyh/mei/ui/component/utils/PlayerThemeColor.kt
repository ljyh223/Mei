package com.ljyh.mei.ui.component.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.media3.common.C
import com.ljyh.mei.di.repository.ColorRepository
import com.ljyh.mei.playback.PlayerConnection
import com.ljyh.mei.utils.cache.preloadImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
        val player = connection.player

        connection.service.currentMediaMetadata.collectLatest { song ->
            if (song == null) return@collectLatest
            themeColor = colorRepository.getColorOrExtract(context, song.coverUrl)

            val nextIndex = player.nextMediaItemIndex
            if (nextIndex != C.INDEX_UNSET) {
                val nextUrl = player.getMediaItemAt(nextIndex)
                    .mediaMetadata
                    .artworkUri
                    ?.toString()
                if (!nextUrl.isNullOrEmpty()) {
                    launch(Dispatchers.IO) {
                        colorRepository.getColorOrExtract(context, nextUrl)
                        preloadImage(context, nextUrl)
                    }
                }
            }
        }
    }

    return themeColor
}

package com.ljyh.mei.ui.component.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.ljyh.mei.constants.DynamicThemeKey
import com.ljyh.mei.ui.local.LocalDatabase
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.rememberPreference
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

@Composable
fun DynamicThemeHandler(
    content: @Composable (seedColor: Color) -> Unit
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current

    val loader = rememberNetworkLoader()
    val dominantColorState = rememberDominantColorState(loader)
    val isColorLoaded = remember { mutableStateOf(false) } // 记录颜色是否已从数据库加载
    var coverUrl by remember { mutableStateOf("") }
    val dynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
    // 这两个 LaunchedEffect 专门处理颜色逻辑
    LaunchedEffect(playerConnection, dynamicTheme) {
        if (!dynamicTheme) return@LaunchedEffect
        playerConnection?.service?.currentMediaMetadata?.collectLatest { song ->
            if (dynamicTheme && song != null) {
                coverUrl = song.coverUrl
                withContext(Dispatchers.IO) {
                    val cachedColor = database.colorDao().getColor(song.coverUrl)
                    if (cachedColor != null) {
                        // 不能直接赋值 color，改用 updateFrom 触发更新
                        withContext(Dispatchers.Main) {
                            dominantColorState.updateFrom(Url(song.coverUrl))
                            isColorLoaded.value = true // 颜色已加载，避免重复存储
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            val demoImageUrl = Url(song.coverUrl)
                            loader.load(demoImageUrl)
                            dominantColorState.updateFrom(demoImageUrl)
                            isColorLoaded.value = false // 颜色未加载，将在下一个 LaunchedEffect 存入数据库
                        }
                    }
                }


            }


        }
    }

    LaunchedEffect(dominantColorState.color, coverUrl, dynamicTheme) {
        if (!dynamicTheme) return@LaunchedEffect
        if (coverUrl.isNotEmpty() && !isColorLoaded.value) {
            withContext(Dispatchers.IO) {
                database.colorDao().insertColor(
                    com.ljyh.mei.data.model.room.CacheColor(
                        url = coverUrl,
                        color = dominantColorState.color.toArgb()
                    )
                )
            }
            isColorLoaded.value = true
        }
    }

    val seedColor =
        if (dynamicTheme) dominantColorState.color else MaterialTheme.colorScheme.primary
    content(seedColor)
}

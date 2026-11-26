package com.ljyh.mei.ui.screen.album

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Shuffle
import com.ljyh.mei.ui.component.layout.LocalPhoneLandscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.unit.dp
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButton
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButtonStyle
import com.ljyh.mei.ui.component.menu.MeiMenuItem
import com.ljyh.mei.utils.share.MediaShareLink

@Composable
internal fun AlbumDetailMoreButton(
    state: AlbumDetailUiState,
    actions: AlbumDetailActions,
    onSearchActiveChange: (Boolean) -> Unit = actions.onSearchActiveChange,
    additionalItems: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    var chooseArtwork by remember { mutableStateOf(false) }
    val landscape = LocalPhoneLandscape.current
    val artworkLabel = state.artworkMode.label + if (state.artworkMode == AlbumArtworkMode.Model) {
        " · ${state.modelMode.label}"
    } else ""
    MeiDetailMoreButton(
        shareContent = MediaShareLink.album(state.album), searchActive = state.isSearchActive,
        searchLabel = "搜索专辑歌曲", onSearchActiveChange = onSearchActiveChange,
        style = if (landscape) MeiDetailMoreButtonStyle.Artwork else MeiDetailMoreButtonStyle.Toolbar,
        additionalItems = { dismiss ->
            MeiMenuItem(
                when (state.isSubscribed) {
                    true -> "取消收藏专辑"
                    false -> "收藏专辑"
                    null -> if (state.isSubscriptionLoading) "正在获取收藏状态" else "重试获取收藏状态"
                },
                if (state.isSubscribed == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                enabled = state.canSubscribe,
            ) { dismiss(); actions.onSubscribe() }
            MeiMenuItem("封面展示 · $artworkLabel", Icons.Rounded.Image) {
                dismiss(); chooseArtwork = true
            }
            if (landscape) {
                MeiMenuItem("随机播放专辑", Icons.Rounded.Shuffle, enabled = state.canPlay && state.songs.isNotEmpty()) {
                    dismiss(); actions.onShuffle()
                }
                MeiMenuItem("下载专辑", Icons.Rounded.Download, enabled = state.songs.isNotEmpty()) {
                    dismiss(); actions.onDownload()
                }
            }
            additionalItems?.invoke(dismiss)
        },
    )
    if (chooseArtwork) AlertDialog(
        onDismissRequest = { chooseArtwork = false }, title = { Text("专辑封面展示") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                Text("应用于所有专辑详情。动态封面沿用外观设置中的开关与来源。",
                    style = MaterialTheme.typography.bodySmall)
                if (landscape) Text("手机横屏仅显示年代模型或静态封面，竖屏仍使用所选设置。",
                    style = MaterialTheme.typography.bodySmall)
                AlbumArtworkMode.entries.forEach { mode ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(state.artworkMode == mode, role = Role.RadioButton) {
                            actions.onArtworkModeChange(mode)
                            if (mode != AlbumArtworkMode.Model) chooseArtwork = false
                        }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(state.artworkMode == mode, onClick = null)
                        Text(mode.label, Modifier.padding(start = 12.dp))
                    }
                    if (mode == AlbumArtworkMode.Model && state.artworkMode == AlbumArtworkMode.Model) {
                        Column(Modifier.padding(start = 24.dp).selectableGroup()) {
                            AlbumModelMode.entries.forEach { model ->
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .selectable(state.modelMode == model, role = Role.RadioButton) {
                                        actions.onModelModeChange(model); chooseArtwork = false
                                    }, verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(state.modelMode == model, onClick = null)
                                    Text(model.label, Modifier.padding(start = 12.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { chooseArtwork = false }) { Text("关闭") } },
    )
}

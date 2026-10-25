package com.ljyh.mei.ui.component.track

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.ui.component.menu.MeiMenuItem
import com.ljyh.mei.constants.DebugKey
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.share.MediaShareLink
import com.ljyh.mei.utils.share.shareMediaLink
import com.ljyh.mei.utils.system.setClipboard

data class TrackMenuTarget(val track: MediaMetadata, val anchor: IntRect, val source: QueueSource? = null)

private class TrackMenuPositionProvider(
    private val button: IntRect,
    private val margin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val x = (button.right - popupContentSize.width).coerceIn(margin, maxX)
        val preferredY = if (button.bottom + popupContentSize.height <= windowSize.height - margin) {
            button.bottom
        } else {
            button.top - popupContentSize.height
        }
        return IntOffset(x, preferredY.coerceIn(margin, maxY))
    }
}

@Composable
fun TrackActionMenu(
    target: TrackMenuTarget?,
    isCreator: Boolean = false,
    onDismiss: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownloadTrack: (() -> Unit)? = null,
    onCompleteMetadata: (() -> Unit)? = null,
    onDelete: () -> Unit = {},
    onCopyId: () -> Unit,
    onCopyName: () -> Unit,
) {
    val track = target?.track ?: return
    val context = LocalContext.current
    val density = LocalDensity.current
    val debugEnabled by rememberPreference(DebugKey, defaultValue = false)
    val maxHeight = (LocalConfiguration.current.screenHeightDp.dp - 32.dp).coerceIn(120.dp, 360.dp)
    val margin = with(density) { 8.dp.roundToPx() }

    Popup(
        onDismissRequest = onDismiss,
        popupPositionProvider = TrackMenuPositionProvider(target.anchor, margin),
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier.width(232.dp).heightIn(max = maxHeight),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MeiMenuItem("下一首播放", Icons.Rounded.SkipNext) { onDismiss(); onPlayNext() }
                MeiMenuItem("队尾播放", Icons.Rounded.Add) { onDismiss(); onAddToQueue() }
                onAddToPlaylist?.let { action ->
                    MeiMenuItem("添加到歌单", Icons.Rounded.Add) { onDismiss(); action() }
                }
                onDownloadTrack?.let { action ->
                    MeiMenuItem("下载", Icons.Filled.Download) { onDismiss(); action() }
                }
                onCompleteMetadata?.let { action ->
                    MeiMenuItem("补全歌曲信息", Icons.Rounded.Add) { onDismiss(); action() }
                }
                if (isCreator) {
                    MeiMenuItem("从歌单删除", Icons.Rounded.DeleteSweep) { onDismiss(); onDelete() }
                }
                MediaShareLink.song(track)?.let { content ->
                    HorizontalDivider()
                    MeiMenuItem("分享链接", Icons.Rounded.Share) {
                        onDismiss()
                        context.shareMediaLink(content)
                    }
                    MeiMenuItem("复制链接", Icons.Rounded.ContentCopy) {
                        onDismiss()
                        setClipboard(context, content.url, "歌曲链接")
                    }
                }
                HorizontalDivider()
                MeiMenuItem("复制歌名", Icons.Rounded.ContentCopy) { onDismiss(); onCopyName() }
                MeiMenuItem("复制歌曲 ID", Icons.Rounded.ContentCopy) { onDismiss(); onCopyId() }
                if (debugEnabled) {
                    HorizontalDivider()
                    val debugFields = listOf(
                        "歌手" to track.artists.joinToString(" / ") { it.name },
                        "歌手 ID" to track.artists.joinToString(", ") { it.id.toString() },
                        "专辑" to track.album.title,
                        "专辑 ID" to track.album.id.toString(),
                        "封面地址" to track.coverUrl,
                    )
                    debugFields.forEach { (label, value) ->
                        MeiMenuItem("复制$label", Icons.Rounded.ContentCopy) {
                            onDismiss()
                            setClipboard(context, value, label)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun BasicTrackActionMenu(
    target: TrackMenuTarget?,
    onCompleteMetadata: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val track = target?.track
    TrackActionMenu(
        target = target,
        onDismiss = onDismiss,
        onCompleteMetadata = onCompleteMetadata,
        onPlayNext = {
            if (playerConnection == null) {
                Toast.makeText(context, "播放器未就绪", Toast.LENGTH_SHORT).show()
            } else if (track != null) {
                playerConnection.playNext(track.toMediaItem(), target.source)
                Toast.makeText(context, "已添加到下一首", Toast.LENGTH_SHORT).show()
            }
        },
        onAddToQueue = {
            if (playerConnection == null) {
                Toast.makeText(context, "播放器未就绪", Toast.LENGTH_SHORT).show()
            } else if (track != null) {
                playerConnection.addToQueue(track.toMediaItem(), target.source)
                Toast.makeText(context, "已添加到队尾", Toast.LENGTH_SHORT).show()
            }
        },
        onCopyName = { track?.let { setClipboard(context, it.title, "歌名") } },
        onCopyId = { track?.let { setClipboard(context, it.id.toString(), "歌曲 ID") } },
    )
}

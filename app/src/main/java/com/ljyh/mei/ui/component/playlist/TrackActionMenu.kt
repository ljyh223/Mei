package com.ljyh.mei.ui.component.playlist

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.domain.MediaMetadata
import com.ljyh.mei.data.model.domain.toMediaItem
import com.ljyh.mei.ui.component.GridMenu
import com.ljyh.mei.ui.component.GridMenuItem
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.utils.system.setClipboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionMenu(
    targetTrack: MediaMetadata?,
    isCreator: Boolean = false,
    onDismiss: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onPlayNext: () -> Unit,
    onDownloadTrack: (() -> Unit)? = null,
    onDelete: () -> Unit? = {},
    onCopyId: () -> Unit,
    onCopyName: () -> Unit
) {
    if (targetTrack != null) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            GridMenu(
                contentPadding = PaddingValues(bottom = 48.dp)
            ) {
                onAddToPlaylist?.let { addToPlaylist ->
                    GridMenuItem(
                        icon = Icons.Rounded.Add,
                        title = "添加到歌单",
                        onClick = {
                            onDismiss()
                            addToPlaylist()
                        }
                    )
                }
                GridMenuItem(
                    icon = Icons.Rounded.SkipNext,
                    title = "下一首播放",
                    onClick = {
                        onDismiss()
                        onPlayNext()
                    },
                )

                onDownloadTrack?.let { downloadFunc ->
                    GridMenuItem(
                        icon = Icons.Filled.Download,
                        title = "下载",
                        onClick = {
                            onDismiss()
                            downloadFunc()
                        }
                    )
                }

                if (isCreator) {
                    GridMenuItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "删除此歌曲",
                        onClick = {
                            onDismiss()
                            onDelete()
                        }
                    )
                }
                GridMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "复制歌名",
                    onClick = { onDismiss(); onCopyName() })
                GridMenuItem(
                    icon = Icons.Rounded.ContentCopy,
                    title = "复制ID",
                    onClick = { onDismiss(); onCopyId() })
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun BasicTrackActionMenu(track: MediaMetadata?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    TrackActionMenu(
        targetTrack = track,
        onDismiss = onDismiss,
        onPlayNext = {
            if (playerConnection == null) {
                Toast.makeText(context, "播放器未就绪", Toast.LENGTH_SHORT).show()
            } else if (track != null) {
                playerConnection.playNext(track.toMediaItem())
                Toast.makeText(context, "已添加到下一首", Toast.LENGTH_SHORT).show()
            }
        },
        onCopyName = { track?.let { setClipboard(context, it.title, "歌名") } },
        onCopyId = { track?.let { setClipboard(context, it.id.toString(), "歌曲 ID") } },
    )
}

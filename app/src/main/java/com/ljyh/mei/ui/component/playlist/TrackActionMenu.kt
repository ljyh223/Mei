package com.ljyh.mei.ui.component.playlist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.ui.component.GridMenu
import com.ljyh.mei.ui.component.GridMenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackActionMenu(
    targetTrack: MediaMetadata?,
    isCreator: Boolean = false,
    onDismiss: () -> Unit,
    onAddToPlaylist: () -> Unit,
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
                GridMenuItem(
                    icon = Icons.Rounded.Add,
                    title = "添加到歌单",
                    onClick = {
                        onDismiss()
                        onAddToPlaylist()
                    }
                )

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
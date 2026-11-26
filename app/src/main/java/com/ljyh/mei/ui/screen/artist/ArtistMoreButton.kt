package com.ljyh.mei.ui.screen.artist

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButton
import com.ljyh.mei.ui.component.menu.MeiDetailMoreButtonStyle
import com.ljyh.mei.ui.component.menu.MeiMenuItem
import com.ljyh.mei.utils.share.MediaShareLink

@Composable
internal fun ArtistMoreButton(state: ArtistDetailUiState, actions: ArtistDetailActions, inToolbar: Boolean = true, onInfo: () -> Unit) {
    MeiDetailMoreButton(
        style = if (inToolbar) MeiDetailMoreButtonStyle.Toolbar else MeiDetailMoreButtonStyle.Plain,
        shareContent = state.id.toLongOrNull()?.let { MediaShareLink.artist(it, state.artist?.name.orEmpty()) },
        additionalItems = { dismiss ->
            MeiMenuItem("全部歌曲", Icons.AutoMirrored.Rounded.QueueMusic, enabled = state.artist != null) {
                dismiss(); actions.onBrowseSongs()
            }
            MeiMenuItem("全部专辑", Icons.Rounded.Album, enabled = state.artist != null) {
                dismiss(); actions.onBrowseAlbums()
            }
            DropdownMenuItem(text = { Text("歌手简介") }, enabled = state.artist != null,
                leadingIcon = { Icon(painterResource(R.drawable.ic_artist_info), null) },
                onClick = { dismiss(); onInfo() })
        },
    )
}

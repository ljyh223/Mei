package com.ljyh.mei.ui.component.player

import com.ljyh.mei.data.model.domain.MediaMetadata
import androidx.compose.ui.unit.IntRect

sealed interface OverlayState {
    data object None : OverlayState
    data object Playlist : OverlayState
    data object SleepTimer : OverlayState
    data object CreatePlaylist: OverlayState
    data class AddToPlaylist(val mediaId: Long) : OverlayState
    data object MusicQualitySelection : OverlayState
    data class QQMusicSelection(
        val mediaMetadata: MediaMetadata
    ): OverlayState

    data class AlbumArtist(val album: MediaMetadata.Album, val artists: List<MediaMetadata.Artist>,val cover:String): OverlayState
    data class SongInfo(val metadata: MediaMetadata): OverlayState
    data object MoreAction: OverlayState
    data object BottomAction: OverlayState

    data class TrackActionMenu(val track: MediaMetadata, val anchor: IntRect = IntRect.Zero): OverlayState
}

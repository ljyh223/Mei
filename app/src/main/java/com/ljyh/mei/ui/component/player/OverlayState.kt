package com.ljyh.mei.ui.component.player

import com.ljyh.mei.data.model.MediaMetadata
import com.ljyh.mei.data.model.qq.u.SearchResult
import com.ljyh.mei.data.network.Resource

sealed interface OverlayState {
    data object None : OverlayState
    data object Playlist : OverlayState
    data object SleepTimer : OverlayState
    data object CreatePlaylist: OverlayState
    data class AddToPlaylist(val mediaId: Long) : OverlayState
    data class MusicQualitySelection(val current: Int) : OverlayState
    data class QQMusicSelection(
        val searchResult: Resource<SearchResult>,
        val mediaMetadata: MediaMetadata
    ): OverlayState

    data class AlbumArtist(val album: MediaMetadata.Album, val artists: List<MediaMetadata.Artist>,val cover:String): OverlayState
    data object MoreAction: OverlayState
    data object BottomAction: OverlayState

    data class TrackActionMenu(val track: MediaMetadata): OverlayState
}
package com.ljyh.mei.ui.screen.playlist

import com.ljyh.mei.data.model.domain.MediaMetadata

data class PlaylistOrderItem(val id: Long, val track: MediaMetadata?)

val PlaylistOrderItem.displayTrack: MediaMetadata
    get() = track ?: MediaMetadata(
        id = id,
        title = "歌曲暂不可用",
        coverUrl = "",
        artists = emptyList(),
        duration = 0,
        album = MediaMetadata.Album(0, ""),
    )

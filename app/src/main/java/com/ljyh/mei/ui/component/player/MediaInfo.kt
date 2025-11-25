package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.saveable.Saver


data class MediaInfo(
    val id: String = "",
    val cover: String = "", // Assuming cover is a URL or file path
    val album: String = "",
    val artist: String = "",
    val title: String = "",
)

val MediaInfoSaver: Saver<MediaInfo, *> = Saver(
    save = { mediaInfo ->
        // Save MediaInfo as a Bundle-compatible Map
        mapOf(
            "cover" to mediaInfo.cover,
            "album" to mediaInfo.album,
            "artist" to mediaInfo.artist,
            "title" to mediaInfo.title,
        )
    },
    restore = { restored ->
        // Restore MediaInfo from the saved Map
        MediaInfo(
            cover = restored["cover"].toString(),
            album = restored["album"].toString(),
            artist = restored["artist"].toString(),
            title = restored["title"].toString(),
        )
    }
)
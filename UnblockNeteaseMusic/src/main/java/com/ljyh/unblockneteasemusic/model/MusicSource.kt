package com.ljyh.unblockneteasemusic.model

data class TrackId(val source: String, val value: String)
data class MusicArtist(val name: String, val id: String? = null)
data class MusicAlbum(
    val name: String,
    val id: String? = null,
    val coverUrl: String? = null,
    val displayName: String = name,
)
data class MusicTrack(
    val id: TrackId,
    val title: String,
    val artists: List<MusicArtist>,
    val album: MusicAlbum?,
    val durationMs: Long,
    val displayTitle: String = title,
    /** Provider-specific playback identifier when it differs from the catalog/lyric ID. */
    val playbackId: String? = null,
)
data class LyricRequest(val track: MusicTrack, val preferWordSynced: Boolean = true)
data class MusicLyrics(
    val original: String,
    val translation: String = "",
    val romanization: String = "",
    val wordSynced: Boolean = false,
)
data class PlayableAudio(
    val url: String,
    val track: MusicTrack,
    val mimeType: String? = null,
    val bitrate: Int? = null,
)

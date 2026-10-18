package com.ljyh.mei.data.network.qqandroid

import com.ljyh.unblockneteasemusic.model.MusicAlbum
import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.TrackId

/** 搜索命中的一首歌。 */
data class QqSong(
    val id: Long,
    val mid: String,
    val name: String,
    val singer: String,
    val album: String,
    val durationSec: Int,
    val artistNames: List<String> = emptyList(),
    val albumId: String? = null,
    val albumPmid: String? = null,
)

fun QqSong.toMusicTrack(): MusicTrack = MusicTrack(
    id = TrackId("qq", id.toString()),
    title = name,
    artists = (artistNames.ifEmpty { listOfNotNull(singer.takeIf(String::isNotBlank)) })
        .map { MusicArtist(it) },
    album = album.takeIf(String::isNotBlank)?.let { name ->
        MusicAlbum(
            name = name,
            id = albumId,
            coverUrl = albumPmid?.takeIf(String::isNotBlank)
                ?.let { "https://y.qq.com/music/photo_new/T002R300x300M000$it.jpg" },
        )
    },
    durationMs = durationSec.toLong() * 1_000,
    playbackId = mid.takeIf(String::isNotBlank),
)

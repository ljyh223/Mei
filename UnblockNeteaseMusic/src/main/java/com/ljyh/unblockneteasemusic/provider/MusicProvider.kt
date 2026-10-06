package com.ljyh.unblockneteasemusic.provider

import com.ljyh.unblockneteasemusic.model.LyricRequest
import com.ljyh.unblockneteasemusic.model.MusicLyrics
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio

/** Providers implement only the capabilities they support. */
interface MusicCatalogProvider {
    val sourceId: String
    suspend fun search(query: String, limit: Int = 20): List<MusicTrack>
}

interface MusicLyricProvider {
    val sourceId: String
    suspend fun lyrics(request: LyricRequest): MusicLyrics?
}

interface PlayableAudioProvider {
    val sourceId: String
    suspend fun resolve(track: MusicTrack): PlayableAudio?
}

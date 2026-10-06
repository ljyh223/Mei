package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.PlayableAudioProvider
import com.ljyh.unblockneteasemusic.unblock.UnblockResolver
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class UnblockResolverTest {
    @Test
    fun rejectsSameTitleWithWrongArtistAndUsesPlayableMatch() = runBlocking {
        val target = track("netease", "1", "Song", "Artist", 180_000)
        val wrong = track("other", "2", "Song", "Someone Else", 180_000)
        val match = track("other", "3", "Song", "Artist", 182_000)
        val catalog = object : MusicCatalogProvider {
            override val sourceId = "other"
            override suspend fun search(query: String, limit: Int) = listOf(wrong, match)
        }
        val audio = object : PlayableAudioProvider {
            override val sourceId = "other"
            override suspend fun resolve(track: MusicTrack) = PlayableAudio("https://example.test/audio", track)
        }

        val resolved = UnblockResolver(listOf(catalog), listOf(audio)).resolve(target)
        assertEquals("3", resolved?.track?.id?.value)
    }

    private fun track(source: String, id: String, title: String, artist: String, duration: Long) =
        MusicTrack(TrackId(source, id), title, listOf(MusicArtist(artist)), null, duration)
}

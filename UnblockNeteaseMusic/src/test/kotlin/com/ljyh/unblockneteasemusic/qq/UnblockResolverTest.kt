package com.ljyh.unblockneteasemusic.qq

import com.ljyh.unblockneteasemusic.model.MusicArtist
import com.ljyh.unblockneteasemusic.model.MusicTrack
import com.ljyh.unblockneteasemusic.model.PlayableAudio
import com.ljyh.unblockneteasemusic.model.TrackId
import com.ljyh.unblockneteasemusic.provider.MusicCatalogProvider
import com.ljyh.unblockneteasemusic.provider.PlayableAudioProvider
import com.ljyh.unblockneteasemusic.unblock.UnblockResolver
import com.ljyh.unblockneteasemusic.unblock.UnblockEvent
import com.ljyh.unblockneteasemusic.unblock.UnblockStage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun triesNextSourceWhenFirstHasNoPlayableUrl() = runBlocking {
        val target = track("netease", "1", "Song (Cover: Artist)", "Artist", 180_000)
        val events = mutableListOf<UnblockEvent>()
        val first = track("qq", "2", "Song", "Artist", 180_000)
        val second = track("kuwo", "3", "Song", "Artist", 181_000)
        val queried = mutableListOf<String>()
        fun catalog(source: String, candidate: MusicTrack) = object : MusicCatalogProvider {
            override val sourceId = source
            override suspend fun search(query: String, limit: Int): List<MusicTrack> {
                queried += query
                return listOf(candidate)
            }
        }
        fun audio(source: String, url: String?) = object : PlayableAudioProvider {
            override val sourceId = source
            override suspend fun resolve(track: MusicTrack) = url?.let { PlayableAudio(it, track) }
        }

        val result = UnblockResolver(
            listOf(catalog("qq", first), catalog("kuwo", second)),
            listOf(audio("qq", null), audio("kuwo", "https://example.test/song.mp3")),
            onEvent = events::add,
        ).resolve(target)
        assertEquals("kuwo", result?.track?.id?.source)
        assertEquals(listOf("Song - Artist", "Song - Artist"), queried)
        assertEquals(UnblockStage.SELECTED, events.last().stage)
        assertTrue(events.any { it.stage == UnblockStage.URL_UNAVAILABLE && it.sourceId == "qq" })
        assertFalse(events.toString().contains("https://"))
    }

    @Test
    fun rejectsDurationReportedByPlaybackEndpointWhenItDiffers() = runBlocking {
        val target = track("netease", "1", "Song", "Artist", 180_000)
        val candidate = track("migu", "2", "Song", "Artist", 0)
        val catalog = object : MusicCatalogProvider {
            override val sourceId = "migu"
            override suspend fun search(query: String, limit: Int) = listOf(candidate)
        }
        val audio = object : PlayableAudioProvider {
            override val sourceId = "migu"
            override suspend fun resolve(track: MusicTrack) =
                PlayableAudio("https://example.test/song.mp3", track.copy(durationMs = 30_000))
        }
        assertEquals(null, UnblockResolver(listOf(catalog), listOf(audio)).resolve(target))
    }

    private fun track(source: String, id: String, title: String, artist: String, duration: Long) =
        MusicTrack(TrackId(source, id), title, listOf(MusicArtist(artist)), null, duration)
}

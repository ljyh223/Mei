package com.ljyh.mei.playback.lyrics

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadata
import android.media.Rating
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class ColorOsLyricsPublisherTest {
    @Test
    fun preservesHostArtworkIdentityRatingsAndUnknownFields() {
        val cover = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).apply {
            setPixel(0, 0, Color.RED)
            setPixel(1, 1, Color.BLUE)
        }
        val original = metadata("A").let {
            MediaMetadata.Builder(it)
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, cover)
                .putString(MediaMetadata.METADATA_KEY_ART_URI, "content://test/cover")
                .putRating(MediaMetadata.METADATA_KEY_USER_RATING, Rating.newHeartRating(true))
                .putLong("host.number", 42)
                .putString("host.text", "keep")
                .build()
        }
        val session = FakeSession(original)
        val publisher = ColorOsLyricsPublisher(session) { "A" }
        publisher.setLyrics("A", "lyrics-A")

        val published = requireNotNull(session.metadata)
        assertEquals(original.keySet() + COLOR_OS_LYRIC_INFO, published.keySet())
        assertEquals("lyrics-A", published.getString(COLOR_OS_LYRIC_INFO))
        assertEquals("A", published.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
        assertEquals(123_000L, published.getLong(MediaMetadata.METADATA_KEY_DURATION))
        assertEquals(42L, published.getLong("host.number"))
        assertEquals("keep", published.getString("host.text"))
        assertEquals("content://test/cover", published.getString(MediaMetadata.METADATA_KEY_ART_URI))
        assertTrue(requireNotNull(published.getRating(MediaMetadata.METADATA_KEY_USER_RATING)).hasHeart())
        assertTrue(cover.sameAs(published.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)))
        assertNull(original.getString(COLOR_OS_LYRIC_INFO))
    }

    @Test
    fun waitsForBothPlayerAndPlatformToAgreeOnTheTrack() {
        var current = "B"
        val session = FakeSession(metadata("A"))
        val publisher = ColorOsLyricsPublisher(session) { current }
        publisher.setLyrics("B", "lyrics-B")
        assertEquals(0, session.writes)
        session.hostUpdate(metadata("B"))
        assertEquals("lyrics-B", session.metadata?.getString(COLOR_OS_LYRIC_INFO))

        current = "C"
        publisher.setLyrics("B", "late-B")
        assertNull(session.metadata?.getString(COLOR_OS_LYRIC_INFO))
        session.hostUpdate(metadata("C"))
        assertNull(session.metadata?.getString(COLOR_OS_LYRIC_INFO))
    }

    @Test
    fun reappliesAfterHostRefreshWithoutRepeatedWritesOrRestoringOldArtwork() {
        val session = FakeSession(metadata("A"))
        val publisher = ColorOsLyricsPublisher(session) { "A" }
        publisher.setLyrics("A", "lyrics-A")
        publisher.setLyrics("A", "lyrics-A")
        assertEquals(1, session.writes)
        val updated = MediaMetadata.Builder(metadata("A"))
            .putString(MediaMetadata.METADATA_KEY_ART_URI, "content://test/new-cover").build()
        session.hostUpdate(updated)
        assertEquals(2, session.writes)
        assertEquals("lyrics-A", session.metadata?.getString(COLOR_OS_LYRIC_INFO))
        assertEquals("content://test/new-cover", session.metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI))
    }

    @Test
    fun clearsOldSongImmediatelyAndDoesNotOverwriteForeignLyricsWhenClearing() {
        var current = "A"
        val session = FakeSession(metadata("A"))
        val publisher = ColorOsLyricsPublisher(session) { current }
        publisher.setLyrics("A", "lyrics-A")
        current = "B"
        publisher.setLyrics(null, null)
        assertNull(session.metadata?.getString(COLOR_OS_LYRIC_INFO))
        session.hostUpdate(MediaMetadata.Builder(metadata("B")).putString(COLOR_OS_LYRIC_INFO, "foreign").build())
        publisher.setLyrics("B", null)
        assertEquals("foreign", session.metadata?.getString(COLOR_OS_LYRIC_INFO))
    }

    @Test
    fun oversizedMetadataIsLeftUntouched() {
        val original = MediaMetadata.Builder(metadata("A"))
            .putString("host.large", "x".repeat(300_000)).build()
        val session = FakeSession(original)
        val publisher = ColorOsLyricsPublisher(session) { "A" }
        publisher.setLyrics("A", "lyrics-A")
        assertEquals(0, session.writes)
        assertSame(original, session.metadata)
    }

    @Test
    fun closeRemovesOwnedLyricsAndStopsObserving() {
        val session = FakeSession(metadata("A"))
        val publisher = ColorOsLyricsPublisher(session) { "A" }
        publisher.setLyrics("A", "lyrics-A")
        publisher.close()
        publisher.close()
        publisher.setLyrics("A", "late-A")
        assertNull(session.listener)
        assertNull(session.metadata?.getString(COLOR_OS_LYRIC_INFO))
        assertEquals(2, session.writes)
    }

    private fun metadata(id: String) = MediaMetadata.Builder()
        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
        .putString(MediaMetadata.METADATA_KEY_TITLE, "Song $id")
        .putLong(MediaMetadata.METADATA_KEY_DURATION, 123_000).build()

    private class FakeSession(private var value: MediaMetadata?) : LyricMetadataSession {
        override val metadata: MediaMetadata? get() = value
        var writes = 0
        var listener: (() -> Unit)? = null
        override fun setMetadata(value: MediaMetadata) {
            writes++
            hostUpdate(value)
        }
        fun hostUpdate(value: MediaMetadata) {
            this.value = value
            listener?.invoke()
        }
        override fun observe(onChanged: () -> Unit): () -> Unit {
            listener = onChanged
            return { listener = null }
        }
    }
}

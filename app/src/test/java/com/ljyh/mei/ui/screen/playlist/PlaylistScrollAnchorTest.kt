package com.ljyh.mei.ui.screen.playlist

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.*
import org.junit.Test

class PlaylistScrollAnchorTest {
    private val ids = listOf(10L, 20L, 30L)
    @Test fun sameSongAndOffsetMapToDifferentIndicesWhenHeaderMoves() {
        val anchor = PlaylistScrollAnchor(30, 2, 37, false)
        assertEquals(PlaylistAnchorTarget.Item(3, 37), resolvePlaylistAnchor(anchor, ids, true, false, false))
        assertEquals(PlaylistAnchorTarget.Item(2, 37), resolvePlaylistAnchor(anchor, ids, false, false, false))
    }
    @Test fun anchorRoundTripRestoresSongIdentityInsteadOfTheOldLayoutIndex() {
        val anchor = PlaylistScrollAnchor(20, 1, 14, false)
        val scope = object : SaverScope { override fun canBeSaved(value: Any) = true }
        val encoded = with(PlaylistScrollAnchorSaver) { scope.save(anchor) }!!
        val restored = PlaylistScrollAnchorSaver.restore(encoded)!!
        assertEquals(anchor, restored)
        assertEquals(PlaylistAnchorTarget.Item(1, 14), resolvePlaylistAnchor(restored, ids, false, false, false))
    }
    @Test fun headerScrollHasAnExplicitFallbackWhenHeaderBecomesASeparatePane() {
        val anchor = PlaylistScrollAnchor(offset = 210)
        assertEquals(PlaylistAnchorTarget.Item(0, 210), resolvePlaylistAnchor(anchor, ids, true, false, false))
        assertEquals(PlaylistAnchorTarget.Item(0, 0), resolvePlaylistAnchor(anchor, ids, false, false, false))
    }
    @Test fun loadingWaitsAndPaginationLoadsUntilTheSongArrives() {
        val anchor = PlaylistScrollAnchor(30, 2, 12, false)
        assertEquals(PlaylistAnchorTarget.Wait, resolvePlaylistAnchor(anchor, emptyList(), true, true, true, ids))
        assertEquals(PlaylistAnchorTarget.LoadMore, resolvePlaylistAnchor(anchor, ids.take(1), true, false, true, ids))
        assertEquals(PlaylistAnchorTarget.Item(3, 12), resolvePlaylistAnchor(anchor, ids, true, false, false, ids))
    }
    @Test fun missingSongUsesNearestOrdinalAndClearsItsOldPixelOffset() {
        val anchor = PlaylistScrollAnchor(20, 1, 91, false)
        assertEquals(PlaylistAnchorTarget.Item(2, 0), resolvePlaylistAnchor(anchor, listOf(10, 30), true, false, false, listOf(10, 30)))
        assertEquals(PlaylistAnchorTarget.Item(0, 0), resolvePlaylistAnchor(anchor, emptyList(), false, false, false, emptyList()))
    }
    @Test fun deletedSongCanRequireMorePagesToReachItsReplacement() {
        val anchor = PlaylistScrollAnchor(99, 2, 10, false)
        assertEquals(PlaylistAnchorTarget.LoadMore, resolvePlaylistAnchor(anchor, ids.take(1), false, false, true, ids))
        assertEquals(PlaylistAnchorTarget.Item(2, 0), resolvePlaylistAnchor(anchor, ids, false, false, false, ids))
    }
    @Test fun sortingAndFilteringUseTheSongIdBeforeAnOrdinalFallback() {
        val anchor = PlaylistScrollAnchor(30, 2, 9, false)
        assertEquals(PlaylistAnchorTarget.Item(1, 9), resolvePlaylistAnchor(anchor, listOf(30, 10, 20), true, false, false))
        assertEquals(PlaylistAnchorTarget.Item(0, 0), resolvePlaylistAnchor(anchor, listOf(10), false, false, false))
    }
    @Test fun temporaryRowsDoNotOverwriteASavedSong() {
        val previous = PlaylistScrollAnchor(30, 2, 37, false)
        for (key in listOf(null, "playlist_empty", "playlist_track_placeholder_0"))
            assertNull(capturePlaylistAnchor(previous, key, 0, 0, true, false))
    }
    @Test fun remeasuringTwoPaneAtTheTopDoesNotDiscardThePreviousHeaderPosition() {
        val header = PlaylistScrollAnchor(offset = 210)
        assertNull(capturePlaylistAnchor(header, 10L, 0, 0, true, false))
        assertEquals(PlaylistScrollAnchor(10, 0, 0, false), capturePlaylistAnchor(header, 10L, 0, 0, true, true))
    }

}

package com.ljyh.mei.playback

import android.app.Application
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.test.core.app.ApplicationProvider
import com.ljyh.mei.constants.LoopPlaybackKey
import com.ljyh.mei.playback.transition.TransitionDeckPlayer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class PlaybackRepeatModeTest {
    @Test
    fun unsetLoopPreferenceMatchesTheEnabledSettingsDefault() {
        assertTrue(emptyPreferences().loopPlaybackEnabled())
        assertFalse(preferencesOf(LoopPlaybackKey to false).loopPlaybackEnabled())
        assertTrue(preferencesOf(LoopPlaybackKey to true).loopPlaybackEnabled())
    }

    @Test
    fun restoredListRepeatModeUsesTheCurrentLoopPreference() {
        assertEquals(Player.REPEAT_MODE_ALL, playbackRepeatMode(true, Player.REPEAT_MODE_OFF))
        assertEquals(Player.REPEAT_MODE_OFF, playbackRepeatMode(false, Player.REPEAT_MODE_ALL))
    }

    @Test
    fun changingLoopPreferencePreservesSingleRepeatAndFmBehavior() {
        for (enabled in listOf(false, true)) {
            assertEquals(Player.REPEAT_MODE_ONE, playbackRepeatMode(enabled, Player.REPEAT_MODE_ONE))
            assertEquals(Player.REPEAT_MODE_ALL, playbackRepeatMode(enabled, isFmMode = true))
        }
    }

    @Test
    fun lastShuffledSongWrapsToTheFirstShuffledSongWithDefaultLoopSetting() {
        withShuffledPlayer { player ->
            player.applyLoopPlayback(emptyPreferences().loopPlaybackEnabled())

            assertTrue(player.hasNextMediaItem())
            assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            player.seekToNext()
            assertEquals(2, player.currentMediaItemIndex)
        }
    }

    @Test
    fun changingLoopAtShuffleBoundaryImmediatelyEnablesAndDisablesNext() {
        withShuffledPlayer { player ->
            player.applyLoopPlayback(false)
            assertFalse(player.hasNextMediaItem())
            assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))

            player.applyLoopPlayback(true)
            assertTrue(player.hasNextMediaItem())
            assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))

            player.applyLoopPlayback(false)
            assertFalse(player.hasNextMediaItem())
            assertTrue(player.shuffleModeEnabled)
            assertEquals(1, player.currentMediaItemIndex)
        }
    }

    @Test
    fun transitionDeckAlsoReenablesNextAndWrapsAtTheShuffleBoundary() {
        withShuffledPlayer(useTransitionDeck = true) { player ->
            player.applyLoopPlayback(false)
            assertFalse(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            player.applyLoopPlayback(true)
            assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
            player.seekToNext()
            assertEquals(2, player.currentMediaItemIndex)
        }
    }

    private fun withShuffledPlayer(useTransitionDeck: Boolean = false, block: (Player) -> Unit) {
        val player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext<Application>()).build()
        var exposed: Player = player
        try {
            player.setMediaItems((0..2).map { MediaItem.fromUri("https://example.invalid/$it") })
            player.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(intArrayOf(2, 0, 1), 42))
            player.shuffleModeEnabled = true
            // The shuffled last item is the middle item in the original playlist.
            player.seekTo(1, 0)
            if (useTransitionDeck) {
                val standby = ExoPlayer.Builder(ApplicationProvider.getApplicationContext<Application>()).build()
                exposed = TransitionDeckPlayer(player, standby, AudioAttributes.DEFAULT)
            }
            block(exposed)
        } finally {
            exposed.release()
        }
    }
}

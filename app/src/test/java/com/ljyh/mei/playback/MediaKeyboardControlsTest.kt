package com.ljyh.mei.playback

import android.app.Application
import android.view.KeyEvent
import androidx.media3.common.Player
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class MediaKeyboardControlsTest {
    @Test
    fun nextAndPreviousExecuteOncePerPress() {
        val fake = RecordingPlayer()
        assertTrue(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_NEXT), fake.player))
        assertTrue(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_NEXT, repeat = 1), fake.player))
        assertTrue(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_NEXT, action = KeyEvent.ACTION_UP), fake.player))
        assertTrue(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PREVIOUS), fake.player))
        assertEquals(listOf("seekToNext", "seekToPrevious"), fake.calls)
    }

    @Test
    fun fastForwardAndRewindAllowHeldKeyRepeats() {
        val fake = RecordingPlayer()
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, repeat = 1), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, action = KeyEvent.ACTION_UP), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_REWIND), fake.player)
        assertEquals(listOf("seekForward", "seekForward", "seekBack"), fake.calls)
    }

    @Test
    fun playPausePreparesIdlePlayerAndDoesNotToggleOnKeyUpOrRepeat() {
        val fake = RecordingPlayer().apply { state = Player.STATE_IDLE }
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, repeat = 2), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, action = KeyEvent.ACTION_UP), fake.player)
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE), fake.player)
        assertEquals(listOf("prepare", "play", "pause"), fake.calls)
    }

    @Test
    fun explicitPlayPauseStopAndSkipAliasesWork() {
        val fake = RecordingPlayer()
        listOf(KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_STOP, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD).forEach { handleMediaKey(key(it), fake.player) }
        assertEquals(listOf("play", "pause", "stop", "seekToNext", "seekToPrevious"), fake.calls)
    }

    @Test
    fun emptyQueueUnsupportedCommandAndOrdinaryKeysFallThrough() {
        val fake = RecordingPlayer()
        for (code in listOf(KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_HEADSETHOOK)) {
            assertFalse(handleMediaKey(key(code), fake.player))
        }
        fake.count = 0
        assertFalse(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_NEXT), fake.player))
        fake.count = 2
        fake.blocked = Player.COMMAND_SEEK_FORWARD
        assertFalse(handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD), fake.player))
        assertTrue(fake.calls.isEmpty())
    }

    @Test
    fun playPauseRestartsAnEndedTrackEvenIfPlayWhenReadyRemainsTrue() {
        val fake = RecordingPlayer().apply { state = Player.STATE_ENDED; playing = true }
        handleMediaKey(key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE), fake.player)
        assertEquals(listOf("seekToDefaultPosition", "play"), fake.calls)
    }

    private fun key(code: Int, action: Int = KeyEvent.ACTION_DOWN, repeat: Int = 0) =
        KeyEvent(0, 0, action, code, repeat)

    private class RecordingPlayer {
        val calls = mutableListOf<String>()
        var count = 2
        var playing = false
        var state = Player.STATE_READY
        var blocked: Int? = null
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, args ->
            when (method.name) {
                "getMediaItemCount" -> count
                "isCommandAvailable" -> args?.first() != blocked
                "getPlayWhenReady" -> playing
                "getPlaybackState" -> state
                "play", "pause", "prepare", "stop", "seekToNext", "seekToPrevious", "seekForward", "seekBack", "seekToDefaultPosition" -> {
                    calls += method.name
                    if (method.name == "play") playing = true
                    if (method.name == "pause") playing = false
                    if (method.name == "prepare") state = Player.STATE_READY
                    null
                }
                else -> error("Unexpected player call: ${method.name}")
            }
        } as Player
    }
}

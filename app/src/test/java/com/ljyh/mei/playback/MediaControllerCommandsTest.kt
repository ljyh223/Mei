package com.ljyh.mei.playback

import android.app.Application
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE, application = Application::class)
class MediaControllerCommandsTest {
    @Test
    fun externalControllersCanControlTransportAndReadMetadata() {
        val commands = mediaControllerCommands(false)
        listOf(Player.COMMAND_PLAY_PAUSE, Player.COMMAND_PREPARE, Player.COMMAND_STOP,
            Player.COMMAND_SEEK_FORWARD, Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, Player.COMMAND_GET_METADATA,
        ).forEach { assertTrue("Missing command $it", commands.contains(it)) }
    }

    @Test
    fun externalTransportAccessDoesNotGrantQueueOrDeviceModification() {
        val commands = mediaControllerCommands(false)
        listOf(Player.COMMAND_CHANGE_MEDIA_ITEMS, Player.COMMAND_SET_MEDIA_ITEM,
            Player.COMMAND_SET_VOLUME, Player.COMMAND_SET_DEVICE_VOLUME,
            Player.COMMAND_RELEASE, Player.COMMAND_SET_REPEAT_MODE,
        ).forEach { assertFalse("Unexpected command $it", commands.contains(it)) }
        assertEquals(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS, mediaControllerCommands(true))
    }
}

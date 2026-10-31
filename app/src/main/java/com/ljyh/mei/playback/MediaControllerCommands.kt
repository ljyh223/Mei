package com.ljyh.mei.playback

import androidx.media3.common.Player
import androidx.media3.session.MediaSession

/** External media controllers can control transport without editing the queue or device volume. */
internal fun mediaControllerCommands(trusted: Boolean): Player.Commands =
    if (trusted) MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
    else MediaSession.ConnectionResult.DEFAULT_UNTRUSTED_PLAYER_COMMANDS.buildUpon()
        .addAll(
            Player.COMMAND_PLAY_PAUSE,
            Player.COMMAND_PREPARE,
            Player.COMMAND_STOP,
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            Player.COMMAND_SEEK_TO_NEXT,
            Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
            Player.COMMAND_SEEK_TO_PREVIOUS,
            Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        ).build()

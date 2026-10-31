package com.ljyh.mei.playback

import android.view.KeyEvent
import androidx.media3.common.Player

/** Only dedicated media keys are intercepted; text input, arrows and focus traversal stay native. */
internal fun handleMediaKey(event: KeyEvent, player: Player): Boolean {
    val command = when (event.keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
        KeyEvent.KEYCODE_MEDIA_PLAY,
        KeyEvent.KEYCODE_MEDIA_PAUSE -> Player.COMMAND_PLAY_PAUSE
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> Player.COMMAND_SEEK_TO_NEXT
        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> Player.COMMAND_SEEK_TO_PREVIOUS
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> Player.COMMAND_SEEK_FORWARD
        KeyEvent.KEYCODE_MEDIA_REWIND -> Player.COMMAND_SEEK_BACK
        KeyEvent.KEYCODE_MEDIA_STOP -> Player.COMMAND_STOP
        else -> return false
    }
    if (player.mediaItemCount == 0) return false
    if (event.action == KeyEvent.ACTION_UP || event.isCanceled) return true
    if (event.action != KeyEvent.ACTION_DOWN) return false
    // Holding play/pause or next must not toggle repeatedly or skip the entire queue.
    if (event.repeatCount > 0 && command != Player.COMMAND_SEEK_FORWARD && command != Player.COMMAND_SEEK_BACK) return true
    if (!player.isCommandAvailable(command)) return false
    when (event.keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> if (player.playWhenReady &&
            player.playbackState != Player.STATE_ENDED && player.playbackState != Player.STATE_IDLE
        ) player.pause() else prepareAndPlay(player)
        KeyEvent.KEYCODE_MEDIA_PLAY -> prepareAndPlay(player)
        KeyEvent.KEYCODE_MEDIA_PAUSE -> player.pause()
        KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD -> player.seekToNext()
        KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD -> player.seekToPrevious()
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> player.seekForward()
        KeyEvent.KEYCODE_MEDIA_REWIND -> player.seekBack()
        KeyEvent.KEYCODE_MEDIA_STOP -> player.stop()
    }
    return true
}

private fun prepareAndPlay(player: Player) {
    if (player.playbackState == Player.STATE_IDLE && player.isCommandAvailable(Player.COMMAND_PREPARE)) player.prepare()
    if (player.playbackState == Player.STATE_ENDED && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
        player.seekToDefaultPosition()
    }
    player.play()
}

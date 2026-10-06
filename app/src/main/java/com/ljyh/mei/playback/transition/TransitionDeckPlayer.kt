package com.ljyh.mei.playback.transition

import androidx.media3.common.AudioAttributes
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Presents one player to MediaSession while two decoders overlap at a song boundary. */
@UnstableApi
class TransitionDeckPlayer(
    first: ExoPlayer,
    second: ExoPlayer,
    private val audioAttributes: AudioAttributes,
) : ForwardingSimpleBasePlayer(first) {
    private val decks = arrayOf(first, second)
    private var current = 0
    private var released = false
    var onQueueEdited: (() -> Unit)? = null
    var onSeekRequested: (() -> Unit)? = null

    val active: ExoPlayer get() = decks[current]
    val standby: ExoPlayer get() = decks[1 - current]

    /** The incoming deck keeps playing during the MediaSession handoff. */
    fun promote(): ExoPlayer {
        val old = active
        val next = standby
        old.setHandleAudioBecomingNoisy(false)
        old.setAudioAttributes(audioAttributes, false)
        next.setAudioAttributes(audioAttributes, true)
        next.setHandleAudioBecomingNoisy(true)
        current = 1 - current
        setPlayer(next)
        return old
    }

    override fun handleSetMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleSetMediaItems(mediaItems, startIndex, startPositionMs)
    }

    override fun handleAddMediaItems(index: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleAddMediaItems(index, mediaItems)
    }

    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleRemoveMediaItems(fromIndex, toIndex)
    }

    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleMoveMediaItems(fromIndex, toIndex, newIndex)
    }

    override fun handleReplaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: List<MediaItem>): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleReplaceMediaItems(fromIndex, toIndex, mediaItems)
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        onSeekRequested?.invoke()
        return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleSetRepeatMode(repeatMode)
    }

    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        onQueueEdited?.invoke()
        return super.handleSetShuffleModeEnabled(shuffleModeEnabled)
    }

    override fun handleRelease(): ListenableFuture<*> {
        if (!released) {
            released = true
            decks.forEach(ExoPlayer::release)
        }
        return Futures.immediateVoidFuture()
    }
}

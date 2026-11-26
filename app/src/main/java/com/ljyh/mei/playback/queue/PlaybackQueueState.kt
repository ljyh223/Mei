package com.ljyh.mei.playback.queue

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import com.ljyh.mei.playback.transition.TransitionDeckPlayer

/** One occurrence of a track; duplicate songs retain independent identity. */
data class PlaybackQueueItem(val entryId: String, val item: MediaItem)

data class PlaybackQueueState(
    val items: List<PlaybackQueueItem> = emptyList(),
    val currentEntryId: String? = null,
    val isShuffleModeEnabled: Boolean = false,
) {
    val currentIndex: Int get() = items.indexOfFirst { it.entryId == currentEntryId }
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun Player.queueOrderIndices(shuffled: Boolean = shuffleModeEnabled): List<Int> {
    val timeline = currentTimeline
    if (!shuffled) return (0 until timeline.windowCount).toList()
    return buildList {
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET && size < timeline.windowCount) {
            add(index)
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
internal fun Player.queueEntryKey(index: Int): String = getMediaItemAt(index).queueEntryId
    ?: "timeline:${currentTimeline.getWindow(index, Timeline.Window()).uid}"

@androidx.annotation.OptIn(UnstableApi::class)
internal fun Player.readQueueState() = PlaybackQueueState(
    items = queueOrderIndices().map { PlaybackQueueItem(queueEntryKey(it), getMediaItemAt(it)) },
    currentEntryId = currentMediaItemIndex.takeIf { it in 0 until mediaItemCount }?.let(::queueEntryKey),
    isShuffleModeEnabled = shuffleModeEnabled,
)

/** Keep the native shuffle adapter here; UI and queue rules do not depend on the two decks. */
@androidx.annotation.OptIn(UnstableApi::class)
internal fun Player.applyQueueShuffleOrder(order: List<Int>) {
    require(order.size == mediaItemCount && order.toSet() == (0 until mediaItemCount).toSet())
    val engine = when (this) {
        is TransitionDeckPlayer -> {
            onQueueEdited?.invoke()
            active
        }
        is ExoPlayer -> this
        else -> error("This player cannot edit shuffle order")
    }
    engine.setShuffleOrder(ShuffleOrder.DefaultShuffleOrder(order.toIntArray(), System.nanoTime()))
}

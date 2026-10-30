package com.ljyh.mei.playback.lyrics

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.ljyh.mei.data.model.domain.metadata
import com.ljyh.mei.extensions.currentMetadata
import com.ljyh.mei.utils.lyric.LyricManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Service-owned loading works with the activity closed and desktop lyrics disabled. */
@UnstableApi
internal class PlaybackLyricsController(
    private val player: Player,
    private val lyricManager: LyricManager,
    private val scope: CoroutineScope,
    private val publish: (String?, String?) -> Unit,
) : Player.Listener {
    private var mediaId: String? = null
    private var nextMediaId: String? = null
    private var job: Job? = null

    fun start() {
        player.addListener(this)
        refresh()
        job = scope.launch {
            // Read the latest data AND its owner synchronously on the player's main thread.
            // A queued flow notification must never pair old lyrics with a new song ID.
            lyricManager.lyricData.collect { publishCurrentLyrics() }
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_TIMELINE_CHANGED,
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                Player.EVENT_REPEAT_MODE_CHANGED,
            )) refresh()
    }

    private fun refresh() {
        val current = player.currentMediaItem?.mediaId
        if (current != mediaId) {
            mediaId = current
            publish(null, null)
        }
        val metadata = player.currentMetadata
        if (metadata != null) lyricManager.loadLyrics(metadata)
        else if (current == null) lyricManager.cancelAll()
        publishCurrentLyrics()

        val nextIndex = player.nextMediaItemIndex
        val next = if (nextIndex != C.INDEX_UNSET) player.getMediaItemAt(nextIndex) else null
        val nextMetadata = next?.metadata
        val nextId = next?.mediaId.takeIf { nextMetadata != null }
        if (nextId != nextMediaId) {
            nextMediaId = nextId
            if (nextMetadata != null) lyricManager.preloadLyrics(nextMetadata)
        }
    }

    private fun publishCurrentLyrics() {
        val current = player.currentMediaItem?.mediaId
        val metadata = player.currentMetadata
        val data = metadata?.let { lyricManager.lyricsForSong(it.id.toString()) }
        val payload = if (current != null && current == mediaId && metadata != null && data != null) {
            colorOsLyricPayload(current, metadata, data)
        } else null
        publish(current, payload)
    }

    fun close() {
        job?.cancel()
        job = null
        player.removeListener(this)
        publish(null, null)
        lyricManager.cancelAll()
    }
}

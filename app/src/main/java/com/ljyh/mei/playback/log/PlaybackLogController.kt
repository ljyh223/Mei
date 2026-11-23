package com.ljyh.mei.playback.log

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.ljyh.mei.playback.queue.QueueSource
import com.ljyh.mei.playback.queue.queueEntryId
import com.ljyh.mei.playback.queue.queueSource
import com.ljyh.mei.playback.source.MediaUriProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Main-looper adapter. Only this owner talks to Player; encoding and I/O run off-thread. */
internal class PlaybackLogController(
    private val repository: PlaybackLogRepository,
    private val sources: MediaUriProvider,
) : Player.Listener {
    private val session = PlaybackLogSession()

    fun attach(player: Player, scope: CoroutineScope) {
        player.addListener(this)
        scope.launch { repository.identity.collect { synchronize(player) } }
        scope.launch { sources.sourceInfo.collect { synchronize(player) } }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        val nextEntry = mediaItem?.queueEntryId ?: mediaItem?.mediaId
        if (nextEntry != session.entryId || reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT ||
            reason == Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) {
            val end = when (reason) {
                Player.MEDIA_ITEM_TRANSITION_REASON_AUTO, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT -> "playend"
                Player.MEDIA_ITEM_TRANSITION_REASON_SEEK -> "ui"
                else -> "interrupt"
            }
            finish(end)
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                Player.EVENT_PLAYER_ERROR, Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_TIMELINE_CHANGED,
            )) synchronize(player)
    }

    fun synchronize(player: Player) {
        if (player.playbackState == Player.STATE_IDLE || player.playbackState == Player.STATE_ENDED) {
            finish(if (player.playerError != null) "exception" else
                if (player.playbackState == Player.STATE_ENDED) "playend" else "interrupt")
            return
        }
        val item = player.currentMediaItem
        val id = item?.mediaId?.toLongOrNull()?.takeIf { it > 0 }
        val source = item?.queueSource
        val track = if (item != null && id != null && source?.type != QueueSource.Type.LOCAL &&
            sources.sourceInfo.value[item.mediaId]?.isNetease == true) {
            val (type, sourceId) = playbackLogSource(source, id)
            PlaybackLogTrack(
                id, item.queueEntryId ?: item.mediaId,
                item.mediaMetadata.title?.toString().orEmpty(),
                item.mediaMetadata.artist?.toString().orEmpty(),
                item.mediaMetadata.durationMs?.takeIf { it > 0 } ?: player.duration.coerceAtLeast(0),
                type, sourceId,
            )
        } else null
        session.update(
            track, repository.identity.value, repository.appSessionId, player.isPlaying,
            player.playbackParameters.speed, SystemClock.elapsedRealtime(), System.currentTimeMillis(),
        ).forEach(repository::record)
    }

    fun finish(reason: String = "interrupt") {
        session.finish(SystemClock.elapsedRealtime(), System.currentTimeMillis(), reason)?.let(repository::record)
    }

    fun detach(player: Player) {
        finish()
        player.removeListener(this)
    }
}

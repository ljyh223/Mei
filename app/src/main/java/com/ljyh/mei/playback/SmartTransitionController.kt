package com.ljyh.mei.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.ljyh.mei.constants.SmartTransitionDurationKey
import com.ljyh.mei.constants.SmartTransitionEnabledKey
import com.ljyh.mei.constants.SmartTransitionMode
import com.ljyh.mei.constants.SmartTransitionModeKey
import com.ljyh.mei.data.model.metadata
import com.ljyh.mei.extensions.mediaItems
import com.ljyh.mei.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import timber.log.Timber

/** Manages two decoders while keeping the public Player and MediaSession stable. */
@UnstableApi
internal class SmartTransitionController(
    context: Context,
    private val decks: TransitionDeckPlayer,
    private val probes: Map<Player, TransitionAudioProbe>,
    private val scope: CoroutineScope,
    private val stopVolumeFade: () -> Unit,
    private val canTransition: () -> Boolean,
    private val resolveSource: suspend (String) -> Uri,
) {
    private data class Settings(
        val enabled: Boolean = false,
        val mode: SmartTransitionMode = SmartTransitionMode.Smart,
        val fixedMs: Long = 6_000L,
    )

    private data class Fade(
        val outgoing: ExoPlayer,
        val incoming: ExoPlayer,
        val startPositionMs: Long,
        val durationMs: Long,
        val target: String,
        val baseSpeed: Float,
        val incomingRate: Float,
    )

    private var settings = Settings()
    private var target: String? = null
    private var targetItem: MediaItem? = null
    private var failedTarget: String? = null
    private var previewStartedAt = 0L
    private var previewFinished = false
    private var incomingEnvelope: List<Float> = emptyList()
    private var fade: Fade? = null
    private val beatNet = BeatNetAnalyzer(context)
    private var analysisJob: Job? = null
    private var analysisAttempt: String? = null
    private var beatPlan: BeatNetTransitionPlan? = null
    private var preparedBeatStartMs = 0L
    val isTransitioning: Boolean get() = fade != null

    private val preferenceJob: Job = scope.launch {
        context.dataStore.data.collectLatest { prefs ->
            val previousMode = settings.mode
            settings = Settings(
                enabled = prefs[SmartTransitionEnabledKey] ?: false,
                mode = runCatching { SmartTransitionMode.valueOf(prefs[SmartTransitionModeKey] ?: "Smart") }
                    .getOrDefault(SmartTransitionMode.Smart),
                fixedMs = (prefs[SmartTransitionDurationKey] ?: 6).coerceIn(2, 12) * 1_000L,
            )
            if (!settings.enabled) cancel()
            else if (previousMode != settings.mode) cancel()
        }
    }
    private val monitorJob: Job = scope.launch {
        while (isActive) {
            try { tick() } catch (error: Exception) {
                Timber.tag("SmartTransition").w(error, "Transition skipped")
                failedTarget = target
                cancel()
            }
            delay(if (settings.enabled && target != null) 100L else 500L)
        }
    }

    init {
        decks.onQueueEdited = { cancel() }
        decks.onSeekRequested = { cancel() }
    }

    private fun tick() {
        fade?.let { updateFade(it); return }
        val active = decks.active
        val nextIndex = active.nextMediaItemIndex
        val duration = active.duration
        val currentIndex = active.currentMediaItemIndex
        val item = if (nextIndex != C.INDEX_UNSET && nextIndex in 0 until active.mediaItemCount)
            active.getMediaItemAt(nextIndex) else null
        val currentAlbum = active.currentMediaItem?.metadata?.album?.id
        val nextAlbum = item?.metadata?.album?.id
        val eligible = settings.enabled && canTransition() &&
            active.repeatMode != Player.REPEAT_MODE_ONE && duration != C.TIME_UNSET && duration > 0 &&
            item != null && item.mediaId != active.currentMediaItem?.mediaId &&
            !(currentAlbum != null && currentAlbum != 0L && currentAlbum == nextAlbum)
        if (!eligible) { if (target != null || analysisAttempt != null) cancel(); return }
        if (!active.isPlaying) {
            if (previewStartedAt != 0L && !previewFinished) {
                decks.standby.pause()
                incomingEnvelope = probes[decks.standby]?.envelope.orEmpty()
                decks.standby.seekTo(0L)
                previewFinished = true
            }
            return
        }
        val key = "$currentIndex:$nextIndex:${item.mediaId}"
        if (target != null && (target != key || targetItem != item)) cancel()
        if (analysisAttempt != null && analysisAttempt != key) clearAnalysis()
        val remaining = duration - active.currentPosition
        if (settings.mode == SmartTransitionMode.Smart && remaining in 1_000L..65_000L &&
            analysisAttempt != key && failedTarget != key
        ) launchAnalysis(key, active.currentMediaItem!!.mediaId, item.mediaId, duration)
        if (remaining > 25_000L || remaining < 1_000L || failedTarget == key) return
        if (target == null) prepare(key, item, nextIndex)
        val incoming = decks.standby
        if (incoming.playerError != null) {
            failedTarget = key
            cancel()
            return
        }
        if (incoming.playbackState != Player.STATE_READY) return

        val rhythmPlan = beatPlan
        if (settings.mode == SmartTransitionMode.Smart && rhythmPlan != null) {
            if (preparedBeatStartMs != rhythmPlan.incomingStartMs) {
                incoming.pause()
                incoming.seekTo(rhythmPlan.incomingStartMs)
                preparedBeatStartMs = rhythmPlan.incomingStartMs
                previewFinished = true
                return
            }
            if (active.currentPosition >= rhythmPlan.outgoingStartMs && remaining > 2_000L) {
                beginFade(key, TransitionPlan(min(rhythmPlan.durationMs, remaining - 150L),
                    rhythmPlan.incomingStartMs), rhythmPlan.incomingRate)
            }
            return
        }

        if (settings.mode == SmartTransitionMode.Smart && !previewFinished && remaining > 12_000L) {
            if (previewStartedAt == 0L) {
                incoming.volume = 0f
                incoming.play()
                previewStartedAt = android.os.SystemClock.elapsedRealtime()
            } else if (android.os.SystemClock.elapsedRealtime() - previewStartedAt >= 2_500L) {
                incoming.pause()
                incomingEnvelope = probes[incoming]?.envelope.orEmpty()
                incoming.seekTo(0L)
                previewFinished = true
            }
            return
        }
        if (previewStartedAt != 0L && !previewFinished) {
            incoming.pause()
            incomingEnvelope = probes[incoming]?.envelope.orEmpty()
            incoming.seekTo(0L)
            previewFinished = true
        }
        val plan = if (settings.mode == SmartTransitionMode.Smart) {
            SmartTransitionPlanner.plan(probes[active]?.envelope.orEmpty(), incomingEnvelope, remaining)
        } else TransitionPlan(settings.fixedMs)
        if (remaining <= plan.durationMs + 150L) beginFade(key, plan)
    }

    private fun prepare(key: String, item: MediaItem, index: Int) {
        val active = decks.active
        val incoming = decks.standby
        target = key
        targetItem = item
        previewStartedAt = 0L
        previewFinished = false
        incomingEnvelope = emptyList()
        incoming.pause()
        incoming.volume = 0f
        preparedBeatStartMs = beatPlan?.incomingStartMs ?: 0L
        if (beatPlan != null) previewFinished = true
        incoming.setMediaItems(active.mediaItems.toList(), index, preparedBeatStartMs)
        incoming.setShuffleOrder(active.shuffleOrder)
        incoming.shuffleModeEnabled = active.shuffleModeEnabled
        incoming.repeatMode = active.repeatMode
        incoming.playbackParameters = active.playbackParameters
        incoming.skipSilenceEnabled = active.skipSilenceEnabled
        incoming.prepare()
    }

    private fun beginFade(key: String, plan: TransitionPlan, incomingRate: Float = 1f) {
        val outgoing = decks.active
        val incoming = decks.standby
        if (incoming.playbackState != Player.STATE_READY || !outgoing.isPlaying) return
        stopVolumeFade()
        outgoing.volume = 1f
        outgoing.setPauseAtEndOfMediaItems(true)
        if (plan.incomingStartMs > 0L && abs(incoming.currentPosition - plan.incomingStartMs) > 100L) {
            incoming.seekTo(plan.incomingStartMs)
        }
        val baseSpeed = outgoing.playbackParameters.speed
        if (incomingRate != 1f) incoming.playbackParameters = PlaybackParameters(baseSpeed * incomingRate)
        incoming.volume = 0f
        fade = Fade(outgoing, incoming, outgoing.currentPosition, plan.durationMs, key,
            baseSpeed, incomingRate)
        incoming.play()
    }

    private fun updateFade(state: Fade) {
        if (decks.active !== state.outgoing || target != state.target || !canTransition()) {
            cancel()
            return
        }
        state.incoming.playWhenReady = state.outgoing.playWhenReady
        if (state.incoming.playerError != null) {
            failedTarget = state.target
            cancel()
            return
        }
        val progress = ((state.outgoing.currentPosition - state.startPositionMs).toFloat() /
            state.durationMs).coerceIn(0f, 1f)
        val speed = state.baseSpeed * (state.incomingRate + (1f - state.incomingRate) * progress)
        if (abs(state.incoming.playbackParameters.speed - speed) > 0.005f) {
            state.incoming.playbackParameters = PlaybackParameters(speed)
        }
        state.outgoing.volume = cos(progress * PI / 2).toFloat()
        state.incoming.volume = sin(progress * PI / 2).toFloat()
        if (progress >= 1f || state.outgoing.playbackState == Player.STATE_ENDED ||
            state.outgoing.currentPosition >= state.outgoing.duration - 150L
        ) finish(state)
    }

    private fun finish(state: Fade) {
        state.incoming.volume = 1f
        state.incoming.playbackParameters = PlaybackParameters(state.baseSpeed)
        state.incoming.playWhenReady = state.outgoing.playWhenReady
        decks.promote()
        fade = null
        state.outgoing.stop()
        state.outgoing.clearMediaItems()
        state.outgoing.setPauseAtEndOfMediaItems(false)
        state.outgoing.volume = 0f
        target = null
        targetItem = null
        previewStartedAt = 0L
        previewFinished = false
        incomingEnvelope = emptyList()
        clearAnalysis()
    }

    private fun cancel() {
        fade = null
        decks.active.setPauseAtEndOfMediaItems(false)
        decks.active.volume = 1f
        decks.standby.pause()
        decks.standby.stop()
        decks.standby.clearMediaItems()
        decks.standby.volume = 0f
        target = null
        targetItem = null
        previewStartedAt = 0L
        previewFinished = false
        incomingEnvelope = emptyList()
        clearAnalysis()
    }

    private fun clearAnalysis() {
        analysisJob?.cancel()
        analysisJob = null
        analysisAttempt = null
        beatPlan = null
    }

    private fun launchAnalysis(key: String, outgoingId: String, incomingId: String, durationMs: Long) {
        analysisAttempt = key
        analysisJob = scope.launch {
            try {
                val outgoingUri = resolveSource(outgoingId)
                val incomingUri = resolveSource(incomingId)
                val result = beatNet.plan(outgoingId, outgoingUri, durationMs, incomingId, incomingUri)
                if (analysisAttempt == key) beatPlan = result
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.tag("SmartTransition").w(error, "BeatNet analysis unavailable; using audio envelope")
            } finally {
                if (analysisAttempt == key) analysisJob = null
            }
        }
    }

    fun release() {
        preferenceJob.cancel()
        monitorJob.cancel()
        decks.onQueueEdited = null
        decks.onSeekRequested = null
        cancel()
    }
}

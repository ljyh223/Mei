package com.ljyh.mei.playback.log

/** Immutable queue occurrence captured when audible playback starts. */
internal data class PlaybackLogTrack(
    val id: Long,
    val entryId: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val source: String,
    val sourceId: String,
)

internal data class PlaybackLogEvent(
    val track: PlaybackLogTrack,
    val owner: String,
    val appSessionId: String,
    val startedAtMs: Long,
    val atMs: Long,
    val realTimeMs: Long = 0,
    val playedTimeMs: Long = 0,
    val end: String? = null,
)

/** Only elapsed audible time advances; seeks and wall-clock corrections cannot inflate it. */
internal class PlaybackLogSession {
    private var active: PlaybackLogEvent? = null
    private var runningSince: Long? = null
    private var speed = 1.0
    private var realTime = 0L
    private var playedTime = 0.0

    val entryId: String? get() = active?.track?.entryId

    fun update(
        track: PlaybackLogTrack?,
        owner: String?,
        appSessionId: String,
        isPlaying: Boolean,
        playbackSpeed: Float,
        elapsedMs: Long,
        wallMs: Long,
    ): List<PlaybackLogEvent> = buildList {
        val previous = active
        if (previous != null && (track?.entryId != previous.track.entryId ||
                track?.id != previous.track.id || owner != previous.owner)) {
            finish(elapsedMs, wallMs)?.let(::add)
        }
        accrue(elapsedMs)
        if (active == null && track != null && owner != null && isPlaying) {
            val event = PlaybackLogEvent(track, owner, appSessionId, wallMs, wallMs)
            active = event
            add(event)
        }
        speed = playbackSpeed.toDouble().takeIf { it.isFinite() && it > 0 } ?: 1.0
        runningSince = elapsedMs.takeIf { isPlaying && active != null }
    }

    fun finish(elapsedMs: Long, wallMs: Long, reason: String = "interrupt"): PlaybackLogEvent? {
        accrue(elapsedMs)
        val result = active?.copy(
            atMs = wallMs,
            realTimeMs = realTime,
            playedTimeMs = playedTime.toLong(),
            end = reason,
        )
        active = null
        realTime = 0
        playedTime = 0.0
        return result
    }

    private fun accrue(elapsedMs: Long) {
        runningSince?.let { since ->
            val delta = (elapsedMs - since).coerceAtLeast(0)
            realTime += delta
            playedTime += delta * speed
        }
        runningSince = null
    }
}

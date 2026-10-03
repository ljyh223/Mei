package com.ljyh.mei.playback

import kotlin.math.abs

/** Fills the gaps between coarse player position updates with the frame clock. */
internal class SmoothPlaybackPosition {
    private var anchorPositionMs = 0L
    private var anchorTimeNanos = 0L
    private var previousRawPositionMs = Long.MIN_VALUE
    private var previousOutputMs = 0L
    private var wasPlaying = false
    private var previousSpeed = 1f

    fun reset() {
        previousRawPositionMs = Long.MIN_VALUE
        wasPlaying = false
    }

    fun sample(
        rawPositionMs: Long,
        frameTimeNanos: Long,
        isPlaying: Boolean,
        speed: Float,
        durationMs: Long,
    ): Int {
        val end = durationMs.coerceIn(0L, Int.MAX_VALUE.toLong())
        val raw = rawPositionMs.coerceIn(0L, end)
        val playbackSpeed = speed.takeIf { it.isFinite() && it > 0f } ?: 1f
        val reanchor = previousRawPositionMs == Long.MIN_VALUE || !isPlaying || !wasPlaying ||
            playbackSpeed != previousSpeed || frameTimeNanos < anchorTimeNanos

        if (reanchor) {
            anchorPositionMs = raw
            anchorTimeNanos = frameTimeNanos
            previousOutputMs = raw
        } else if (raw != previousRawPositionMs) {
            val predicted = predictedPosition(frameTimeNanos, playbackSpeed)
            val error = raw - predicted
            anchorPositionMs = if (abs(error) > 400L) {
                previousOutputMs = raw // Seek or media transition: use the player immediately.
                raw
            } else {
                predicted + error.coerceIn(-16L, 16L)
            }
            anchorTimeNanos = frameTimeNanos
        }

        previousRawPositionMs = raw
        wasPlaying = isPlaying
        previousSpeed = playbackSpeed
        val estimate = if (isPlaying) predictedPosition(frameTimeNanos, playbackSpeed) else raw
        val result = if (isPlaying) {
            estimate.coerceAtMost((raw + 320L).coerceAtMost(end))
                .coerceAtLeast(previousOutputMs)
        } else raw
        previousOutputMs = result
        return result.coerceIn(0L, end).toInt()
    }

    private fun predictedPosition(nowNanos: Long, speed: Float): Long =
        anchorPositionMs + ((nowNanos - anchorTimeNanos).coerceAtLeast(0L) / 1_000_000.0 * speed).toLong()
}

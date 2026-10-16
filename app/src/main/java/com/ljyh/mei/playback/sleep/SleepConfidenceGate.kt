package com.ljyh.mei.playback.sleep

/** Requires two fresh, consecutive high-confidence classifications from this session. */
internal class SleepConfidenceGate(private val startedAtMillis: Long) {
    private var lastTimestampMillis = 0L
    private var consecutiveHigh = 0

    fun onClassification(timestampMillis: Long, confidence: Int, nowMillis: Long): Boolean {
        if (timestampMillis < startedAtMillis || timestampMillis <= lastTimestampMillis) return false
        if (timestampMillis > nowMillis + 60_000L || nowMillis - timestampMillis > 30 * 60_000L) return false

        if (lastTimestampMillis > 0 && timestampMillis - lastTimestampMillis > 30 * 60_000L) {
            consecutiveHigh = 0
        }
        lastTimestampMillis = timestampMillis
        consecutiveHigh = if (confidence >= 80) consecutiveHigh + 1 else 0
        return consecutiveHigh >= 2
    }
}

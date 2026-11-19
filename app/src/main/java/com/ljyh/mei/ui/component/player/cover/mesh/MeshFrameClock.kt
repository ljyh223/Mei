package com.ljyh.mei.ui.component.player.cover.mesh

/** Animation time excludes periods spent static, paused or without a visible surface. */
internal class MeshFrameClock {
    private var static = false
    private var playing = true
    private var previousNanos: Long? = null
    private var elapsedNanos = 0L

    fun setStaticMode(enabled: Boolean) {
        if (static == enabled) return
        static = enabled
        resetFrameTime()
    }

    fun setPlaying(enabled: Boolean) {
        if (playing == enabled) return
        playing = enabled
        resetFrameTime()
    }

    fun resetFrameTime() {
        previousNanos = null
    }

    fun timeSeconds(nowNanos: Long): Float {
        val previous = previousNanos
        if (!static && playing && previous != null) {
            elapsedNanos += (nowNanos - previous).coerceAtLeast(0L)
        }
        previousNanos = nowNanos
        return elapsedNanos / 1e9f
    }

    fun needsAnotherFrame(hasArtwork: Boolean, transitioning: Boolean): Boolean =
        hasArtwork && (transitioning || (!static && playing))
}

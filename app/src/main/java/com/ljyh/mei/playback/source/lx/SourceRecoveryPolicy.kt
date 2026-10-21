package com.ljyh.mei.playback.source.lx

/** Bounded, temporary exclusions survive READY/error cycles but expire for later listening. */
internal class SourceRecoveryPolicy(private val now: () -> Long = System::currentTimeMillis) {
    data class Snapshot(val skipNetease: Boolean = false, val excluded: Set<String> = emptySet())
    private data class State(
        val startedAt: Long, var attempts: Int = 0, var refreshed: Boolean = false,
        var skipNetease: Boolean = false, val excluded: MutableSet<String> = mutableSetOf(),
    )
    private val states = mutableMapOf<String, State>()
    @Synchronized fun snapshot(mediaId: String): Snapshot {
        expire()
        val state = states[mediaId] ?: return Snapshot()
        return Snapshot(state.skipNetease, state.excluded.toSet())
    }
    @Synchronized fun failed(mediaId: String, identity: String): Boolean {
        expire()
        if (states.size >= 128 && mediaId !in states) states.keys.firstOrNull()?.let { states.remove(it) }
        val state = states.getOrPut(mediaId) { State(now()) }
        state.attempts++
        if (identity == "netease") {
            if (state.refreshed) state.skipNetease = true else state.refreshed = true
        } else {
            state.skipNetease = true
            state.excluded += identity
        }
        return state.attempts <= 6
    }
    private fun expire() { states.entries.removeAll { now() - it.value.startedAt >= 5 * 60_000 } }
}

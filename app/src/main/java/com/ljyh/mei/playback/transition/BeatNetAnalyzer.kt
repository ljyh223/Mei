package com.ljyh.mei.playback.transition

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

/** Analyzes the end and start of two songs with the published BeatNet model. */
internal class BeatNetAnalyzer(context: Context) {
    private val decoder = BeatNetAudioDecoder(context)
    private val features = BeatNetFeatures()
    private val runtime by lazy { BeatNetRuntime(context) }
    private val mutex = Mutex()
    private val cache = object : LinkedHashMap<String, BeatNetRhythm>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BeatNetRhythm>?) = size > 8
    }

    suspend fun plan(
        outgoingId: String,
        outgoingUri: Uri,
        outgoingDurationMs: Long,
        incomingId: String,
        incomingUri: Uri,
    ): BeatNetTransitionPlan? = mutex.withLock {
        val outgoingStart = (outgoingDurationMs - 32_000L).coerceAtLeast(0L)
        val outgoing = analyze("$outgoingId@$outgoingStart@$outgoingUri", outgoingUri, outgoingStart)
        val incoming = analyze("$incomingId@0@$incomingUri", incomingUri, 0L)
        BeatNetRhythmPlanner.plan(outgoing, incoming, outgoingDurationMs)
    }

    private suspend fun analyze(key: String, uri: Uri, startMs: Long): BeatNetRhythm {
        cache[key]?.let { return it }
        val pcm = decoder.decode(uri, startMs)
        currentCoroutineContext().ensureActive()
        val activations = withContext(Dispatchers.Default) {
            runtime.run(features.extract(pcm.samples, pcm.sampleRate))
        }
        currentCoroutineContext().ensureActive()
        val rhythm = requireNotNull(BeatNetRhythmPlanner.decode(activations, startMs)) {
            "BeatNet did not find a stable pulse"
        }
        cache[key] = rhythm
        return rhythm
    }
}

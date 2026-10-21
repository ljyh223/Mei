package com.ljyh.mei.playback.source.lx

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** Priority is a head start, never a requirement to wait for a slower high-priority source. */
internal suspend fun <I, O : Any> firstUsable(
    inputs: List<I>,
    staggerMs: Long = 150,
    timeoutMs: Long = 18_000,
    resolve: suspend (I) -> O?,
): O? = coroutineScope {
    if (inputs.isEmpty()) return@coroutineScope null
    val results = Channel<O?>(inputs.size)
    val permits = Semaphore(3)
    val jobs = inputs.mapIndexed { index, input ->
        launch {
            var result: O? = null
            try {
                delay(index * staggerMs)
                result = permits.withPermit {
                    try { withTimeout(15_000) { resolve(input) } }
                    catch (_: TimeoutCancellationException) { null }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { /* One script failure does not cancel the race. */ }
            finally { results.trySend(result) }
        }
    }
    try {
        withTimeoutOrNull(timeoutMs) {
            repeat(inputs.size) { results.receive()?.let { return@withTimeoutOrNull it } }
            null
        }
    } finally { jobs.forEach(Job::cancel); results.close() }
}

package com.ljyh.mei.playback.log

import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** No cookies or credentials: only records, opaque owner hashes and envelope randomness. */
internal class PlaybackLogOutbox(file: File, private val maxBytes: Int = 2 * 1024 * 1024) {
    private val file = AtomicFile(file)
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun add(log: PendingPlaybackLog) = change { rows ->
        (rows.filterNot { it.id == log.id } + log).takeLast(MAX_RECORDS)
    }

    suspend fun pending(owner: String?, nowMs: Long): List<PendingPlaybackLog> = change { rows ->
        rows.filter { it.owner == owner && nowMs - it.createdAtMs <= MAX_AGE_MS }
    }

    suspend fun accepted(id: String, channel: String) = change { rows ->
        rows.map { if (it.id == id) it.copy(accepted = it.accepted + channel) else it }
            .filterNot { it.accepted.containsAll(CHANNELS) }
    }

    suspend fun failed(id: String) = change { rows ->
        rows.map { if (it.id == id) it.copy(attempts = it.attempts + 1) else it }
            .filter { it.attempts < MAX_ATTEMPTS }
    }

    suspend fun remove(id: String) = change { it.filterNot { row -> row.id == id } }

    private suspend fun change(
        transform: (List<PendingPlaybackLog>) -> List<PendingPlaybackLog>,
    ): List<PendingPlaybackLog> = withContext(Dispatchers.IO) {
        lock.withLock {
            val old = if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) {
                runCatching {
                    file.openRead().use { input ->
                        require(input.available() <= maxBytes)
                        json.decodeFromString<List<PendingPlaybackLog>>(input.readBytes().decodeToString())
                    }
                }.getOrDefault(emptyList())
            } else emptyList()
            var next = transform(old)
            var encoded = json.encodeToString(next).toByteArray()
            while (encoded.size > maxBytes && next.isNotEmpty()) {
                next = next.drop(1)
                encoded = json.encodeToString(next).toByteArray()
            }
            file.baseFile.parentFile?.mkdirs()
            val output = file.startWrite()
            try {
                output.write(encoded)
                file.finishWrite(output)
            } catch (failure: Throwable) {
                file.failWrite(output)
                throw failure
            }
            next
        }
    }

    companion object {
        const val MAX_RECORDS = 256
        const val MAX_ATTEMPTS = 8
        const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        val CHANNELS = listOf("ua", "bi")
    }
}

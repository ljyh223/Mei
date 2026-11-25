package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.eapi.HomePageResourceShow
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Keep a recommendation day until 06:00 Beijing time, independent of the device time zone. */
internal object HomeFeedCachePolicy {
    private val zone = ZoneId.of("Asia/Shanghai")

    fun expiresAt(savedAt: Long): Long {
        val saved = Instant.ofEpochMilli(savedAt).atZone(zone)
        val cutoff = saved.toLocalDate().atTime(6, 0).atZone(zone)
        return (if (saved.isBefore(cutoff)) cutoff else cutoff.plusDays(1)).toInstant().toEpochMilli()
    }

    fun isFresh(savedAt: Long, now: Long): Boolean =
        savedAt > 0 && now >= savedAt && now < expiresAt(savedAt)
}

@Serializable
internal data class CachedHomeFeed(val savedAt: Long, val data: HomePageResourceShow.Data)

/** Persist the first-page timestamp so loading another page cannot extend an old recommendation day. */
internal class HomeFeedCache(private val directory: File, private val json: Json) {
    private val memory = ConcurrentHashMap<String, CachedHomeFeed>()
    private val writes = Any()

    fun peek(user: String): CachedHomeFeed? = memory[user]

    fun read(user: String): CachedHomeFeed? = synchronized(writes) {
        memory[user]?.let { return@synchronized it }
        val current = File(directory, "home_feed_v6_${user.hashCode()}.json")
        val legacy = File(directory, "home_feed_v5_${user.hashCode()}.json")
        val cached = runCatching {
            if (current.exists()) json.decodeFromString<CachedHomeFeed>(current.readText()) else null
        }.getOrNull() ?: runCatching {
            if (legacy.exists()) CachedHomeFeed(
                legacy.lastModified(), json.decodeFromString<HomePageResourceShow.Data>(legacy.readText()),
            ) else null
        }.getOrNull()
        cached?.also { memory[user] = it }
    }

    fun store(user: String, data: HomePageResourceShow.Data, savedAt: Long): Unit = synchronized(writes) {
        val cached = CachedHomeFeed(savedAt, data)
        memory[user] = cached
        // The in-memory response remains usable when the disk is full or temporarily unavailable.
        runCatching {
            File(directory, "home_feed_v6_${user.hashCode()}.json").writeText(json.encodeToString(cached))
        }
        Unit
    }

    fun storeNextPage(user: String, previous: HomePageResourceShow.Data, combined: HomePageResourceShow.Data): Unit = synchronized(writes) {
        // Do not cache a late page over a newer refresh, or invent a new first-page timestamp.
        val cached = memory[user]?.takeIf { it.data === previous } ?: return@synchronized
        store(user, combined, cached.savedAt)
    }
}

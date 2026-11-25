package com.ljyh.mei.data.repository

import com.ljyh.mei.data.model.eapi.HomePageResourceShow.Data
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class HomeFeedCacheTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private fun time(value: String) = LocalDateTime.parse(value).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()
    private fun page(code: String) = Data(listOf(Data.Block(positionCode = code)))

    @Test fun eveningCacheLastsUntilExactlySixTheFollowingMorning() {
        val saved = time("2026-11-19T22:00:00")
        val cutoff = time("2026-11-20T06:00:00")
        assertEquals(cutoff, HomeFeedCachePolicy.expiresAt(saved))
        assertTrue(HomeFeedCachePolicy.isFresh(saved, time("2026-11-20T05:59:59")))
        assertFalse(HomeFeedCachePolicy.isFresh(saved, cutoff))
    }

    @Test fun earlyMorningCacheBelongsToThePreviousRecommendationDay() {
        val saved = time("2026-11-20T02:00:00")
        assertEquals(time("2026-11-20T06:00:00"), HomeFeedCachePolicy.expiresAt(saved))
        assertTrue(HomeFeedCachePolicy.isFresh(saved, time("2026-11-20T05:00:00")))
    }

    @Test fun aCacheCreatedAtSixStartsANewFullRecommendationDay() {
        val saved = time("2026-11-20T06:00:00")
        assertEquals(time("2026-11-21T06:00:00"), HomeFeedCachePolicy.expiresAt(saved))
        assertTrue(HomeFeedCachePolicy.isFresh(saved, time("2026-11-20T23:00:00")))
        assertFalse(HomeFeedCachePolicy.isFresh(saved, time("2026-11-21T06:00:00")))
    }

    @Test fun deviceTimeZoneAndMonthBoundariesDoNotChangeTheBeijingCutoff() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val saved = time("2026-12-31T23:00:00")
            assertEquals(time("2027-01-01T06:00:00"), HomeFeedCachePolicy.expiresAt(saved))
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun timestampsFromTheFutureOrWithoutASaveTimeAreNotTreatedAsFresh() {
        val now = time("2026-11-20T05:00:00")
        assertFalse(HomeFeedCachePolicy.isFresh(0, now))
        assertFalse(HomeFeedCachePolicy.isFresh(now + 1, now))
    }

    @Test fun paginationKeepsTheFirstPageTimestampAcrossProcessRestarts() {
        val directory = Files.createTempDirectory("mei-home-cache").toFile()
        try {
            val cache = HomeFeedCache(directory, json)
            val first = page("first")
            val saved = time("2026-11-19T22:00:00")
            cache.store("account", first, saved)
            val combined = first.copy(blocks = first.blocks + Data.Block(positionCode = "next"))
            cache.storeNextPage("account", first, combined)
            val reloaded = requireNotNull(HomeFeedCache(directory, json).read("account"))
            assertEquals(saved, reloaded.savedAt)
            assertEquals(listOf("first", "next"), reloaded.data.blocks.map { it.positionCode })
            assertFalse(HomeFeedCachePolicy.isFresh(reloaded.savedAt, time("2026-11-20T06:01:00")))
        } finally { directory.deleteRecursively() }
    }

    @Test fun aLatePageCannotReplaceANewerFirstPageCache() {
        val directory = Files.createTempDirectory("mei-home-cache").toFile()
        try {
            val cache = HomeFeedCache(directory, json)
            val previous = page("old")
            val current = page("new")
            val saved = time("2026-11-20T06:01:00")
            cache.store("account", previous, time("2026-11-19T22:00:00"))
            cache.store("account", current, saved)
            cache.storeNextPage("account", previous, page("late"))
            val reloaded = requireNotNull(HomeFeedCache(directory, json).read("account"))
            assertEquals("new", reloaded.data.blocks.single().positionCode)
            assertEquals(saved, reloaded.savedAt)
        } finally { directory.deleteRecursively() }
    }

    @Test fun oldCacheFormatStillUsesItsExistingTimestampWithoutSharingAccounts() {
        val directory = Files.createTempDirectory("mei-home-cache").toFile()
        try {
            val file = java.io.File(directory, "home_feed_v5_${"account".hashCode()}.json")
            file.writeText(json.encodeToString(page("legacy")))
            val saved = time("2026-11-19T22:00:00")
            assertTrue(file.setLastModified(saved))
            val cache = HomeFeedCache(directory, json)
            assertEquals(saved, cache.read("account")!!.savedAt)
            assertNull(cache.read("other-account"))
        } finally { directory.deleteRecursively() }
    }
}

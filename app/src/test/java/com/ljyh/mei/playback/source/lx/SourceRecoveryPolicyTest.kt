package com.ljyh.mei.playback.source.lx

import org.junit.Assert.*
import org.junit.Test

class SourceRecoveryPolicyTest {
    @Test fun officialUrlIsRefreshedOnceThenSkipped() {
        val policy = SourceRecoveryPolicy()
        assertTrue(policy.failed("1", "netease"))
        assertFalse(policy.snapshot("1").skipNetease)
        assertTrue(policy.failed("1", "netease"))
        assertTrue(policy.snapshot("1").skipNetease)
    }
    @Test fun badScriptResultIsExcludedWithoutAffectingAnotherSong() {
        val policy = SourceRecoveryPolicy()
        val bad = LxAudio("https://example.com/bad", "script1", "wy", "1", "320k")
        policy.failed("1", bad.identity)
        assertTrue(policy.snapshot("1").skipNetease)
        assertEquals(setOf(bad.identity), policy.snapshot("1").excluded)
        assertTrue(policy.snapshot("2").excluded.isEmpty())
        assertFalse(policy.snapshot("2").skipNetease)
    }
    @Test fun readyErrorCyclesCannotRetryForever() {
        val policy = SourceRecoveryPolicy()
        repeat(6) { assertTrue(policy.failed("1", "source$it")) }
        assertFalse(policy.failed("1", "source7"))
    }
    @Test fun temporaryFailuresExpire() {
        var now = 0L
        val policy = SourceRecoveryPolicy { now }
        policy.failed("1", "script|wy|1|320k")
        now = 5 * 60_000
        assertTrue(policy.snapshot("1").excluded.isEmpty())
        assertFalse(policy.snapshot("1").skipNetease)
        assertTrue(policy.failed("1", "netease"))
    }
}

package com.ljyh.mei.playback.source

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import com.ljyh.mei.playback.source.lx.SourceRecoveryPolicy
import com.ljyh.mei.utils.preferences.AudioSourceMode
import com.ljyh.mei.utils.preferences.AudioSourceModeKey
import com.ljyh.mei.utils.preferences.audioSourceMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AudioSourceModeTest {
    @Test fun existingUsersKeepLxRegardlessOfTheObsoleteTestSwitch() {
        assertEquals(AudioSourceMode.Lx, emptyPreferences().audioSourceMode())
        for (legacySwitch in listOf(false, true)) {
            assertEquals(AudioSourceMode.Lx, preferencesOf(
                booleanPreferencesKey("lxOnlyPlayback") to legacySwitch,
            ).audioSourceMode())
        }
    }

    @Test fun storedModesRoundTripAndUnknownValuesKeepLx() {
        for (mode in AudioSourceMode.entries) {
            assertEquals(mode, preferencesOf(AudioSourceModeKey to mode.name).audioSourceMode())
        }
        assertEquals(AudioSourceMode.Lx, preferencesOf(AudioSourceModeKey to "invalid").audioSourceMode())
    }

    @Test fun offMakesNoAlternativeRequests() = runBlocking {
        val result = AudioSourceMode.Off.resolveAlternative<String>(
            lx = { error("LX must not run when disabled") },
            unblock = { error("Unblocking must not run when disabled") },
        )
        assertNull(result)
    }

    @Test fun successfulModesOnlyCallTheirOwnResolver() = runBlocking {
        assertEquals("lx", AudioSourceMode.Lx.resolveAlternative(
            lx = { "lx" }, unblock = { error("Unblocking must not run in LX mode") },
        ))
        assertEquals("unblock", AudioSourceMode.Unblock.resolveAlternative(
            lx = { error("LX must not run in unblocking mode") }, unblock = { "unblock" },
        ))
    }

    @Test fun anUnavailableSourceNeverFallsThroughToTheOtherMode() = runBlocking {
        assertNull(AudioSourceMode.Lx.resolveAlternative<String>(
            lx = { null }, unblock = { error("LX exhaustion must not enable unblocking") },
        ))
        assertNull(AudioSourceMode.Unblock.resolveAlternative<String>(
            lx = { error("Unblocking exhaustion must not enable LX") }, unblock = { null },
        ))
    }

    @Test fun errorsDoNotStartTheOtherResolver() = runBlocking {
        for (mode in listOf(AudioSourceMode.Lx, AudioSourceMode.Unblock)) {
            val failure = IOException("unavailable")
            val thrown = runCatching {
                mode.resolveAlternative<String>(
                    lx = { if (mode == AudioSourceMode.Lx) throw failure else error("Unexpected LX request") },
                    unblock = { if (mode == AudioSourceMode.Unblock) throw failure else error("Unexpected unblock request") },
                )
            }.exceptionOrNull()
            assertSame(failure, thrown)
        }
    }

    @Test fun cancellationDoesNotStartTheOtherResolver() = runBlocking {
        val cancelled = CancellationException("cancelled")
        val thrown = runCatching {
            AudioSourceMode.Lx.resolveAlternative<String>(
                lx = { throw cancelled }, unblock = { error("Cancellation must not enable unblocking") },
            )
        }.exceptionOrNull()
        assertSame(cancelled, thrown)
    }

    @Test fun changingModeDoesNotCarryFailedCandidatesOrSkipTheOfficialSource() {
        val policy = SourceRecoveryPolicy()
        policy.failed(AudioSourceMode.Lx.recoveryKey("1"), "script|wy|1|320k")
        policy.failed(AudioSourceMode.Unblock.recoveryKey("1"), "legacy|qq|1")
        val off = policy.snapshot(AudioSourceMode.Off.recoveryKey("1"))
        assertFalse(off.skipNetease)
        assertTrue(off.excluded.isEmpty())
        assertEquals(setOf("legacy|qq|1"), policy.snapshot(AudioSourceMode.Unblock.recoveryKey("1")).excluded)
        assertEquals(setOf("script|wy|1|320k"), policy.snapshot(AudioSourceMode.Lx.recoveryKey("1")).excluded)
        assertTrue(policy.snapshot(AudioSourceMode.Lx.recoveryKey("2")).excluded.isEmpty())
    }
}

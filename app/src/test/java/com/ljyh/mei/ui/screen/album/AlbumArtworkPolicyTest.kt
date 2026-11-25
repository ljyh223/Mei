package com.ljyh.mei.ui.screen.album

import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.ui.component.artwork.album.AlbumMediaStyle
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumArtworkPolicyTest {
    private fun date(year: Int) = LocalDate.of(year, 1, 1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

    @Test fun automaticModeUsesModelsThrough2009AndStaticFrom2010() {
        listOf(1950, 1979, 1980, 1989, 1990, 2009).forEach { year ->
            assertEquals("$year", AlbumArtworkKind.Model, albumArtworkKind(AlbumArtworkMode.Auto, date(year), false))
        }
        listOf(2010, 2026).forEach { year ->
            assertEquals("$year", AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Auto, date(year), false))
        }
    }
    @Test fun automaticModePrefersAvailableMotionAcrossAllEras() {
        listOf(null, 0L, date(1969), date(1983), date(2009), date(2026)).forEach {
            assertEquals(AlbumArtworkKind.Motion, albumArtworkKind(AlbumArtworkMode.Auto, it, true))
        }
    }
    @Test fun fixedModesDoNotGetOverriddenByAnAvailableVideoOrDigitalEra() {
        assertEquals(AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Static, date(1983), true))
        assertEquals(AlbumArtworkKind.Model, albumArtworkKind(AlbumArtworkMode.Model, date(2026), true))
        assertEquals(AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Motion, date(1983), false))
        assertEquals(AlbumArtworkKind.Motion, albumArtworkKind(AlbumArtworkMode.Motion, date(1983), true))
    }
    @Test fun unknownAndInvalidYearsFallBackToStaticWhenNoMotionIsAvailable() {
        listOf(null, 0L, Long.MAX_VALUE).forEach { time ->
            listOf(AlbumArtworkMode.Auto, AlbumArtworkMode.Model).forEach { mode ->
                assertEquals(AlbumArtworkKind.Static, albumArtworkKind(mode, time, false))
            }
        }
    }
    @Test fun fixedModelsWorkForEveryEraAndForUnknownReleaseDates() {
        val choices = mapOf(AlbumModelMode.Vinyl to AlbumMediaStyle.Vinyl,
            AlbumModelMode.Cassette to AlbumMediaStyle.Cassette, AlbumModelMode.CD to AlbumMediaStyle.CD)
        for ((choice, style) in choices) for (time in listOf(null, 0L, date(1973), date(1983), date(2000), date(2026))) {
            assertEquals(AlbumArtworkKind.Model, albumArtworkKind(AlbumArtworkMode.Model, time, true, choice))
            assertEquals(style, albumArtworkModelStyle(AlbumArtworkMode.Model, time, choice))
        }
    }
    @Test fun storedFixedModelNeverChangesAutomaticArtworkSelection() {
        for (choice in AlbumModelMode.entries) {
            assertEquals(AlbumArtworkKind.Motion, albumArtworkKind(AlbumArtworkMode.Auto, date(1973), true, choice))
            assertEquals(AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Auto, date(2026), false, choice))
            assertEquals(AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Auto, null, false, choice))
            assertEquals(AlbumMediaStyle.Vinyl, albumArtworkModelStyle(AlbumArtworkMode.Auto, date(1973), choice))
            assertEquals(AlbumMediaStyle.Cassette, albumArtworkModelStyle(AlbumArtworkMode.Auto, date(1983), choice))
            assertEquals(AlbumMediaStyle.CD, albumArtworkModelStyle(AlbumArtworkMode.Auto, date(2000), choice))
        }
    }

    @Test fun phoneLandscapeDisallowsMotionAndUsesStillEraFallbackWithoutChangingThePreference() {
        for (mode in listOf(AlbumArtworkMode.Auto, AlbumArtworkMode.Motion)) {
            assertEquals(AlbumArtworkKind.Model, albumArtworkKind(mode, date(1973), true, allowMotion = false))
            assertEquals(AlbumArtworkKind.Model, albumArtworkKind(mode, date(1983), true, allowMotion = false))
            assertEquals(AlbumArtworkKind.Static, albumArtworkKind(mode, date(2026), true, allowMotion = false))
            assertEquals(AlbumArtworkKind.Static, albumArtworkKind(mode, null, true, allowMotion = false))
            assertEquals(AlbumArtworkKind.Motion, albumArtworkKind(mode, date(1973), true, allowMotion = true))
        }
        assertEquals(AlbumArtworkKind.Model, albumArtworkKind(AlbumArtworkMode.Model, null, true,
            AlbumModelMode.CD, allowMotion = false))
        assertEquals(AlbumArtworkKind.Static, albumArtworkKind(AlbumArtworkMode.Static, date(1973), true, allowMotion = false))
    }

}

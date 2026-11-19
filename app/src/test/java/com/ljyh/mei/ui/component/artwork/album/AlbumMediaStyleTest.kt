package com.ljyh.mei.ui.component.artwork.album

import com.ljyh.mei.data.model.domain.toDomain
import com.ljyh.mei.data.model.response.AlbumDetail
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AlbumMediaStyleTest {
    private fun release(year: Int) = LocalDate.of(year, 1, 1)
        .atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli()

    @Test fun preEpochDatesAndEraBoundariesSelectTheConfirmedFormat() {
        listOf(1950 to AlbumMediaStyle.Vinyl, 1969 to AlbumMediaStyle.Vinyl,
            1979 to AlbumMediaStyle.Vinyl, 1980 to AlbumMediaStyle.Cassette,
            1989 to AlbumMediaStyle.Cassette, 1990 to AlbumMediaStyle.CD,
            2000 to AlbumMediaStyle.CD, 2026 to AlbumMediaStyle.CD).forEach { (year, style) ->
            assertEquals(year, albumReleaseYear(release(year)))
            assertEquals(style, albumMediaStyle(release(year)))
        }
    }

    @Test fun missingZeroAndOutOfRangeDatesUseAnOrdinaryCover() {
        listOf(null, 0L, Long.MIN_VALUE, Long.MAX_VALUE).forEach {
            assertNull(albumReleaseYear(it))
            assertEquals(AlbumMediaStyle.Plain, albumMediaStyle(it))
        }
    }

    @Test fun deviceTimezoneDoesNotChangeAnAlbumAtTheYearBoundary() {
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(1980, albumReleaseYear(release(1980)))
            assertEquals(AlbumMediaStyle.Cassette, albumMediaStyle(release(1980)))
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun detailPayloadAcceptsNullAndMissingReleaseDatesWithoutFailing() {
        for (payload in listOf("""{"album":{}}""", """{"album":{"publishTime":null}}""")) {
            val album = Json.decodeFromString<AlbumDetail>(payload).toDomain().album
            assertEquals(AlbumMediaStyle.Plain, albumMediaStyle(album.publishTime))
        }
        val time = release(1950)
        val album = Json.decodeFromString<AlbumDetail>("""{"album":{"publishTime":$time}}""").toDomain().album
        assertEquals(AlbumMediaStyle.Vinyl, albumMediaStyle(album.publishTime))
    }

}

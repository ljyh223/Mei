package com.ljyh.mei.playback.source.lx

import com.ljyh.mei.data.model.response.SongUrl
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class SongUrlPreviewTest {
    @Test fun urlDurationUsesMillisecondsAndTrialMetadataIsPreserved() {
        val response = Json.decodeFromString<SongUrl>("""{"code":200,"data":[{"id":1,"url":"https://example.com/trial.mp3","time":30000,"level":"standard","freeTrialInfo":{"start":0,"end":30}}]}""")
        val item = response.data.single()
        assertTrue(PreviewPolicy.isPreview(item.time))
        assertNotNull(item.freeTrialInfo)
        assertEquals("standard", item.level)
    }
    @Test fun legacyResponsesCanOmitOptionalDuration() {
        val response = Json.decodeFromString<SongUrl>("""{"data":[{"id":1,"url":null}]}""")
        assertNull(response.data.single().time)
        assertNull(response.data.single().freeTrialInfo)
    }
}

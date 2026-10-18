package com.ljyh.mei.data.network.qqandroid

import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class QqAndroidProtocolTest {
    @Test
    fun clientVersionStaysConsistentAcrossRequestFieldsAndCachedProfiles() {
        val device = QqAndroidDevice.synthetic(osVer = "16", model = "test")
        val currentVersion = QQ_ANDROID_VERSION_CODE.toString()
        assertEquals(currentVersion, device.comm["cv"])
        assertEquals(currentVersion, device.comm["v"])
        assertEquals(currentVersion, device.custom(tsMs = 1234).split('&')[1])

        val oldProfile = device.copy(comm = device.comm + mapOf("cv" to "old", "v" to "old"))
        val refreshed = oldProfile.refreshed(nowMs = 1234)
        assertEquals(currentVersion, refreshed.comm["cv"])
        assertEquals(currentVersion, refreshed.comm["v"])

        val song = QqSong(1, "", "歌名", "歌手", "专辑", 60)
        assertEquals(QQ_ANDROID_VERSION_CODE, QqAndroidParams.lyric(song)["cv"]?.jsonPrimitive?.int)
        assertEquals("QQMusic $currentVersion(android 16)", QqAndroidTransport.USER_AGENT)
    }
}

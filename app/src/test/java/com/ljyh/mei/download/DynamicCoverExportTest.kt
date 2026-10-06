package com.ljyh.mei.download

import com.ljyh.mei.data.repository.DynamicCover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicCoverExportTest {
    @Test
    fun fileNameKeepsSourceAndVariantWhileRemovingPathCharacters() {
        assertEquals(
            "A_B_C_AppleMusic_竖屏.mp4",
            dynamicCoverFileName(" A/B:C ", DynamicCover.Source.APPLE_MUSIC, "竖屏")
        )
        assertEquals(
            "动态封面_网易云_原版.mp4",
            dynamicCoverFileName(" .. ", DynamicCover.Source.NETEASE, "原版")
        )
    }

    @Test
    fun signedMp4UsesDirectDownloadWhileHlsNeedsExport() {
        assertTrue(isDirectMp4("https://dcover.music.126.net/cover.mp4?wsSecret=abc"))
        assertFalse(isDirectMp4("https://video.apple.com/cover.m3u8?token=abc"))
    }
}

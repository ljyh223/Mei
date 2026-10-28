package com.ljyh.mei.utils.image

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Test

class ImageUrlsTest {
    private val source = "https://p2.music.126.net/album/cover.jpg"

    @Test fun `incoming Android thumbnail is replaced rather than stacked`() {
        val url = "$source?imageView=1&thumbnail=1043z1043&type=webp&quality=80&param=500y500"
            .artworkImage(ArtworkSize.Small).toHttpUrl()
        assertEquals(listOf("192z192"), url.queryParameterValues("thumbnail"))
        assertNull(url.queryParameter("param"))
        assertEquals("webp", url.queryParameter("type"))
        assertEquals("80", url.queryParameter("quality"))
    }

    @Test fun `thumbnail normalization is idempotent`() {
        val small = source.artworkImage(ArtworkSize.Small)
        assertEquals(small, small.artworkImage(ArtworkSize.Small))
        assertEquals(small, small.artworkImage(ArtworkSize.Card).artworkImage(ArtworkSize.Small))
    }

    @Test fun `arbitrary dimensions collapse to the shared tiers`() {
        assertEquals(source.artworkImage(ArtworkSize.Small), source.artworkImage(ArtworkSize.forPixels(128)))
        assertEquals(source.artworkImage(ArtworkSize.Card), source.artworkImage(ArtworkSize.forPixels(300)))
        assertEquals(source.artworkImage(ArtworkSize.Card), source.artworkImage(ArtworkSize.forPixels(500)))
        assertEquals(source.artworkImage(ArtworkSize.Detail), source.artworkImage(ArtworkSize.forPixels(768)))
        assertEquals(source.artworkImage(ArtworkSize.Detail), source.artworkImage(ArtworkSize.forPixels(4096)))
        assertEquals(source.artworkImage(ArtworkSize.Small), source.artworkImage(ArtworkSize.forPixels(0)))
    }

    @Test fun `foreign URLs and local resources stay intact`() {
        for (url in listOf("", "/storage/cover.jpg", "content://media/1", "file:///cover.jpg",
            "https://music.126.net.example.com/cover.jpg?param=200y200",
            "https://example.org/cover.jpg?token=test", "https://is1-ssl.mzstatic.com/cover.jpg")) {
            assertEquals(url, url.artworkImage(ArtworkSize.Small))
            assertEquals(url, url.originalArtworkUrl())
        }
    }

    @Test fun `unrelated query parameters and fragments survive`() {
        val url = "$source?foo=a%2Bb&foo=c&crop=0_0_20_20#cover".artworkImage(ArtworkSize.Small).toHttpUrl()
        assertEquals(listOf("a+b", "c"), url.queryParameterValues("foo"))
        assertEquals("0_0_20_20", url.queryParameter("crop"))
        assertEquals("cover", url.fragment)
    }

    @Test fun `original request removes thumbnail and lossy encoding options`() {
        assertEquals(source, source.artworkImage(ArtworkSize.Small).originalArtworkUrl())
        val android = "$source?imageView=1&thumbnail=1043z1043&type=webp&quality=80"
        assertEquals(source, android.originalArtworkUrl())
    }

    @Test fun `animated sources are not implicitly converted to a static frame`() {
        assertEquals("0", "$source?tostatic=1".artworkImage(ArtworkSize.Small).toHttpUrl().queryParameter("tostatic"))
    }
    @Test fun `component pixel bounds choose the next shared tier`() {
        // 48dp at 3x, 160dp at 3x, and 260dp at 3x.
        assertEquals(ArtworkSize.Small, ArtworkSize.forBounds(144, 144))
        assertEquals(ArtworkSize.Card, ArtworkSize.forBounds(480, 480))
        assertEquals(ArtworkSize.Detail, ArtworkSize.forBounds(780, 780))
    }

    @Test fun `rectangular and partially bounded components use the larger known side`() {
        assertEquals(ArtworkSize.Card, ArtworkSize.forBounds(128, 400))
        assertEquals(ArtworkSize.Card, ArtworkSize.forBounds(null, 400))
        assertEquals(ArtworkSize.Small, ArtworkSize.forBounds(128, null))
    }

    @Test fun `unknown or oversized component bounds never opt into an original download`() {
        assertEquals(ArtworkSize.Card, ArtworkSize.forBounds(null, null))
        assertEquals(ArtworkSize.Card, ArtworkSize.forBounds(0, 0))
        assertEquals(ArtworkSize.Detail, ArtworkSize.forBounds(4096, 4096))
    }

    @Test fun `full player keeps 1600 pixels when original artwork is disabled`() {
        val url = source.artworkImage(ArtworkSize.Small).playerArtworkUrl(original = false).toHttpUrl()
        assertEquals("1600z1600", url.queryParameter("thumbnail"))
        assertNull(url.queryParameter("param"))
        assertEquals(ArtworkSize.Detail, ArtworkSize.forBounds(1600, 1600))
    }

    @Test fun `full player original setting strips prior thumbnail parameters`() {
        assertEquals(source, source.playerArtworkUrl(original = false).playerArtworkUrl(original = true))
    }

    @Test fun `encoded watermark pipeline survives thumbnail and original requests intact`() {
        val pipeline = "$source?imageView=1&thumbnail=800y800&enlarge=1%7CimageView=1" +
            "&watermark&type=1&image=b3ZlcmxheS0xLnBuZw==&dx=0&dy=0%7Cwatermark" +
            "&type=1&image=b3ZlcmxheS0yLnBuZw==&dx=0&dy=0%7CimageView=1"
        for (size in ArtworkSize.entries) assertEquals(pipeline, pipeline.artworkImage(size))
        assertEquals(pipeline, pipeline.playerArtworkUrl(original = false))
        assertEquals(pipeline, pipeline.originalArtworkUrl())
    }

    @Test fun `raw and lowercase encoded pipeline separators are preserved`() {
        for (separator in listOf("|", "%7c")) {
            val pipeline = "$source?imageView=1&thumbnail=800y800${separator}imageView=1"
            assertEquals(pipeline, pipeline.artworkImage(ArtworkSize.Small))
            assertEquals(pipeline, pipeline.originalArtworkUrl())
        }
    }

    @Test fun `standalone watermark type is not mistaken for an image format`() {
        val watermark = "$source?watermark&type=1&image=b3ZlcmxheS5wbmc=&dx=0&dy=0"
        assertEquals(watermark, watermark.artworkImage(ArtworkSize.Small))
        assertEquals(watermark, watermark.originalArtworkUrl())
    }

}

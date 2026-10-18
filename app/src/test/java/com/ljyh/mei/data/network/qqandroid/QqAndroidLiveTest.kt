package com.ljyh.mei.data.network.qqandroid

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 打真实接口的连通性测试。
 *
 * 默认跳过（单测不应依赖网络），本地验证时：
 *   QQM_LIVE=1 ./gradlew :app:testDebugUnitTest --tests "*QqAndroidLiveTest*"
 */
class QqAndroidLiveTest {

    @Test
    fun `search and lyric end to end`() = runBlocking {
        assumeTrue("set QQM_LIVE=1 to run", System.getenv("QQM_LIVE") == "1")

        val device = QqAndroidDevice.synthetic(osVer = "16", model = "PLR110")
        val transport = QqAndroidTransport()

        val searchRoot = transport.call(
            device,
            QqAndroidParser.MOD_SEARCH,
            QqAndroidParser.MET_SEARCH,
            QqAndroidParams.search("晴天"),
        )
        val songs = QqAndroidParser.parseSearch(searchRoot)
        println("songs=" + songs.take(5).joinToString { "${it.id}:${it.name}/${it.singer}" })
        assertTrue("至少搜到一首", songs.isNotEmpty())

        val first = songs.first()
        val lyricRoot = transport.call(
            device,
            QqAndroidParser.MOD_LYRIC,
            QqAndroidParser.MET_LYRIC,
            QqAndroidParams.lyric(first),
        )
        val lyrics = QqAndroidParser.parseLyric(lyricRoot)
        println("qrcLen=${lyrics.original.length} transLen=${lyrics.translation.length} wordSynced=${lyrics.wordSynced}")
        println(lyrics.original.take(300))

        assertTrue("应有 QRC 歌词", lyrics.original.isNotBlank())
        assertTrue("应是逐字同步的 QRC", lyrics.wordSynced)
    }

    /** qrc=0 分支返回的是 base64 明文 LRC，验证宽容解码能把两种形态都吃下。 */
    @Test
    fun `lrc mode decodes base64 payload`() = runBlocking {
        assumeTrue("set QQM_LIVE=1 to run", System.getenv("QQM_LIVE") == "1")

        val device = QqAndroidDevice.synthetic(osVer = "16", model = "PLR110")
        val transport = QqAndroidTransport()
        val song = QqSong(id = 97773, mid = "", name = "晴天", singer = "周杰伦", album = "叶惠美", durationSec = 269)

        val root = transport.call(
            device,
            QqAndroidParser.MOD_LYRIC,
            QqAndroidParser.MET_LYRIC,
            QqAndroidParams.lyric(song, preferWordSynced = false),
        )
        val raw = QqAndroidParser.parseLyricRaw(root)
        val decoded = com.ljyh.unblockneteasemusic.qq.QRCUtils.decodeLyric(raw.original)

        println("lrc rawHead=${raw.original.take(24)} wordSynced=${raw.wordSynced}")
        println("lrc decoded=${decoded.take(120)}")
        assertTrue("原始字段非空", raw.original.isNotBlank())
        assertTrue("应解出 LRC 文本", decoded.contains("[00:"))
    }
}

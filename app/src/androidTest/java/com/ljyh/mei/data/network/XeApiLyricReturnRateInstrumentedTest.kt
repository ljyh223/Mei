package com.ljyh.mei.data.network

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ljyh.mei.data.model.response.Lyric
import com.ljyh.mei.di.NetworkModule
import com.ljyh.mei.ui.model.LyricSource
import com.ljyh.mei.ui.model.LyricSourceData
import com.ljyh.mei.utils.lyric.mergeLyrics
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/** Manual live-network check of lyrics that Mei can actually display. */
@RunWith(AndroidJUnit4::class)
class XeApiLyricReturnRateInstrumentedTest {
    @Ignore("XEAPI lyric investigation paused; Mei currently uses /api/song/lyric/v1")
    @Test
    fun reportsDisplayableLyricRateForProvidedSongIds() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext
        val json = NetworkModule.provideNeteaseJson()
        val okHttp = NetworkModule.provideOkHttpClient()
        val http = NetworkModule.provideNeteaseHttp(okHttp, json)
        val xeApiClient = NetworkModule.provideXeApiClient(context, http, json)

        val lyricIds = mutableListOf<String>()
        val pureMusicIds = mutableListOf<String>()
        val emptyIds = mutableListOf<String>()
        val unrenderableIds = mutableListOf<String>()
        val failed = mutableListOf<String>()
        var code200Count = 0

        try {
            SONG_IDS.forEachIndexed { index, id ->
                try {
                    val response = xeApiClient.getLyricV1(id)
                    if (response.code != 200) {
                        failed += "$id:code=${response.code}"
                        Log.w(TAG, "$id: response code=${response.code}")
                    } else {
                        code200Count++
                        when (classify(response)) {
                            Availability.Lyrics -> {
                                lyricIds += id
                                Log.i(TAG, "$id: lyrics (lrc=${response.lrc.lyric.isNotBlank()}, " +
                                    "yrc=${response.yrc?.lyric?.isNotBlank() == true})")
                            }
                            Availability.PureMusic -> {
                                pureMusicIds += id
                                Log.i(TAG, "$id: pure music")
                            }
                            Availability.Empty -> {
                                emptyIds += id
                                Log.w(TAG, "$id: code=200 but lyric fields are empty")
                            }
                            Availability.Unrenderable -> {
                                unrenderableIds += id
                                Log.w(TAG, "$id: lyric text exists but Mei cannot render it")
                            }
                        }
                    }
                } catch (error: Exception) {
                    val reason = error.javaClass.simpleName
                    failed += "$id:$reason"
                    Log.w(TAG, "$id: request/decode/render failed ($reason)", error)
                }

                if (index != SONG_IDS.lastIndex) delay(180)
            }

            Log.i(TAG, "summary: code200=$code200Count/${SONG_IDS.size} " +
                "lyrics=${lyricIds.size} pureMusic=${pureMusicIds.size} " +
                "empty=${emptyIds.size} unrenderable=${unrenderableIds.size} " +
                "failed=${failed.size}; emptyIds=$emptyIds " +
                "unrenderableIds=$unrenderableIds failedIds=$failed")

            val unexpected = emptyIds + unrenderableIds + failed
            assertTrue("Lyrics unavailable for: $unexpected", unexpected.isEmpty())
            assertTrue("No supplied song returned displayable lyrics", lyricIds.isNotEmpty())
        } finally {
            http.client.close()
            okHttp.dispatcher.executorService.shutdown()
        }
    }

    @Test
    fun code200WithoutLyricsIsNotCountedAsSuccess() {
        assertEquals(Availability.Empty, classify(Lyric(code = 200)))
        assertEquals(Availability.PureMusic, classify(Lyric(code = 200, pureMusic = true)))
        assertEquals(
            Availability.Lyrics,
            classify(Lyric(code = 200, lrc = Lyric.Lrc("[00:01.00]line"))),
        )
    }

    private enum class Availability { Lyrics, PureMusic, Empty, Unrenderable }

    private fun classify(response: Lyric): Availability {
        val hasRawLyrics = response.lrc.lyric.isNotBlank() ||
            response.yrc?.lyric?.isNotBlank() == true
        if (!hasRawLyrics) {
            return if (response.pureMusic == true) Availability.PureMusic else Availability.Empty
        }
        val rendered = mergeLyrics(
            sources = listOf(LyricSourceData.NetEase(response)),
            isPureMusic = response.pureMusic == true,
        )
        return if (rendered.source == LyricSource.NetEaseCloudMusic &&
            rendered.lyricLine.lines.isNotEmpty()
        ) Availability.Lyrics else Availability.Unrenderable
    }

    private companion object {
        const val TAG = "XeApiLyricRate"
        val SONG_IDS = listOf(
            "3442644155", "3402127872", "3447749509", "432486474",
            "2111993053", "3395677679", "1383706576", "1950112498",
            "3436763496", "3415865705", "1311427648", "2676697681",
            "2667742552", "3415867798", "3388953397", "2116432036",
            "34766723", "22711357", "26349201", "22803678",
            "2122283431", "27711544", "1428153831", "1361165212",
            "2755377729",
        )
    }
}

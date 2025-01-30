package com.ljyh.music

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ljyh.music", appContext.packageName)

        val result=isWithinSameDayWindow(1736518123739)
        println(result)
    }

    private fun isWithinSameDayWindow(time: Long): Boolean {

        val now = LocalDateTime.now()
        if (time == 0L) {
            return true
        }

        // 将时间戳转换为 LocalDateTime
        val lastRequestTime = LocalDateTime.ofEpochSecond(time, 0, java.time.ZoneOffset.UTC)

        // 获取今天早上6点的时间
        val sixAM = now.toLocalDate().atTime(LocalTime.of(6, 0))

        // 如果上次请求时间在今天早上6点之前，则需要请求新的
        return lastRequestTime.isBefore(sixAM)

    }
}
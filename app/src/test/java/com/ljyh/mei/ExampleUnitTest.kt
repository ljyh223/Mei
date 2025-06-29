package com.ljyh.mei

import org.junit.Test

import org.junit.Assert.*
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
//        val result=isWithinSameDayWindow(1736518123739)
//        println(result)
    }


}

fun main(){
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
package com.ljyh.mei.utils


import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object DateUtils {
    //检查给定的时间是否处于昨天和今天特定的时间落点
    fun isWithinSameDayWindow(time: Long): Boolean {
        if (time == 0L) {
            return true
        }
        val sixAM = getTimestampOfSixAM()
        println("传入时间: ${Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())}")
        println("今天6点: ${Instant.ofEpochMilli(sixAM).atZone(ZoneId.systemDefault())}")
        println("比较结果: ${time < sixAM}")
        return time < sixAM

    }
    private fun getTimestampOfSixAM(): Long {
        val zoneId = ZoneId.of("Asia/Shanghai")  // 或你明确知道的时区
        val today = LocalDate.now(zoneId)
        val sixAM = LocalTime.of(6, 0)
        val zonedDateTime = ZonedDateTime.of(today, sixAM, zoneId)
        return zonedDateTime.toInstant().toEpochMilli()
    }

    //获取时间 MM-dd
    fun getDate(): String {
        val date = LocalDate.now()
        return "${date.monthValue}-${date.dayOfMonth}"
    }

    //根据现在的时候返回不同的问候词
    // 早上 06:00 - 12:00 早上好
    // 中午 12:00 - 14:00 中午好
    // 下午 14:00 - 18:00 下午好
    // 晚上 18:00 - 24:00 晚上好
    // 24:00 - 06:00 夜深了
    fun getGreeting(zoneId: ZoneId = ZoneId.systemDefault()): String {
        val currentTime = LocalTime.now(zoneId)
        return when {
            currentTime.isBefore(LocalTime.of(6, 0)) -> "夜深了"
            currentTime.isBefore(LocalTime.of(12, 0)) -> "早上好"
            currentTime.isBefore(LocalTime.of(14, 0)) -> "中午好"
            currentTime.isBefore(LocalTime.of(18, 0)) -> "下午好"
            else -> "晚上好"
        }
    }

}
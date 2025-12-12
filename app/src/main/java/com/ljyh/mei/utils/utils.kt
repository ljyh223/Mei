package com.ljyh.mei.utils

import com.ljyh.mei.data.model.eapi.HomePageResourceShow


fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}


private fun getPositionPriority(positionCode: String): Int = when (positionCode) {
    "PAGE_RECOMMEND_DAILY_RECOMMEND" -> 1   // 最高优先级
    "PAGE_RECOMMEND_RADAR" -> 2
    "PAGE_RECOMMEND_SPECIAL_CLOUD_VILLAGE_PLAYLIST" -> 3
    "PAGE_RECOMMEND_RANK" -> 4
    "PAGE_RECOMMEND_PRIVATE_RCMD_SONG" -> 5
    else -> Int.MAX_VALUE                   // 其他类型默认置底
}

val positionComparator = Comparator<HomePageResourceShow.Data.Block> { a, b ->
    val aPriority = getPositionPriority(a.positionCode)
    val bPriority = getPositionPriority(b.positionCode)
    aPriority.compareTo(bPriority)          // 数值越小优先级越高
}
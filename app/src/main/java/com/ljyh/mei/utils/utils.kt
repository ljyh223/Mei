package com.ljyh.mei.utils

import com.ljyh.mei.data.model.HomePageResourceShow


fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}


fun <T> shuffleExceptOne(arr: List<T>, fixedIndex: Int): List<T> {
    if (fixedIndex !in arr.indices) return arr.shuffled()          // 越界就纯打乱
    val res = arr.toMutableList()                                  // 复制一份
    val first = res.removeAt(fixedIndex)                           // 抽出指定元素
    res.shuffle()                                                  // 打乱剩余
    res.add(0, first)                                              // 放回头部
    return res
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
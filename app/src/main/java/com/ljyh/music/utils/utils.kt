package com.ljyh.music.utils



fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

fun <T> rearrangeArray(index: Int, arr: List<T>): List<T> {
    if (index !in arr.indices) return arr // 无效 index 直接返回原数组
    return arr.subList(index, arr.size) + arr.subList(0, index)
}


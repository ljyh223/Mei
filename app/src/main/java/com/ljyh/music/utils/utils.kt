package com.ljyh.music.utils



fun reportException(throwable: Throwable) {
    throwable.printStackTrace()
}

fun <T>rearrangeArray(index: Int, arr: List<T>): List<T> {
    // 检查输入是否有效
    if (index < 0 || index >= arr.size) {
        return  arr
    }
    val afterIndex = arr.subList(index + 1, arr.size)
    val beforeIndex = arr.subList(0, index + 1)
    return afterIndex + beforeIndex
}



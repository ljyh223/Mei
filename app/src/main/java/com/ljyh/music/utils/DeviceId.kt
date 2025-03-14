package com.ljyh.music.utils

import com.ljyh.music.AppContext
import java.security.MessageDigest
import java.util.Base64
fun <T> getRandomFromList(list: List<T>): T {
    return list.random()
}

fun getDeviceId(): String {
    val assets=AppContext.instance.assets
    // 读 url
    val inputStream = assets.open("devices.txt")
    val content = inputStream.bufferedReader().use { it.readLines() }
    return getRandomFromList(content)
}



fun cloudmusicDllEncodeId(someId: String, idXorKey1: String): String {
    // Step 1: XOR encryption
    val xoredString = buildString {
        for (i in someId.indices) {
            val charCode = someId[i].code xor idXorKey1[i % idXorKey1.length].code
            append(charCode.toChar())
        }
    }

    // Step 2: Compute MD5 hash
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(xoredString.toByteArray())

    // Step 3: Encode the hash in Base64
    return Base64.getEncoder().encodeToString(digest)
}
package com.ljyh.mei.data.network.netease

import java.security.MessageDigest

/** In-memory authentication snapshot. Never serialize or log it. */
internal class PlaybackLogAccount(
    val userId: String,
    val musicU: String,
    val deviceId: String,
    val profile: Map<String, String>,
) {
    val owner: String = playbackLogOwner(userId, musicU, deviceId)
    override fun toString(): String = "PlaybackLogAccount(redacted)"
}

internal fun playbackLogOwner(userId: String, musicU: String, deviceId: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("$userId\u0000$musicU\u0000$deviceId".toByteArray())
        .joinToString("") { "%02x".format(it.toInt() and 255) }

package com.ljyh.mei.utils.netease

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AndroidIdKey
import com.ljyh.mei.constants.AndroidUserAgent
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.encrypt.encryptId
import com.ljyh.mei.utils.encrypt.generateRandomMac
import com.ljyh.mei.utils.get
import korlibs.encoding.Base64
import kotlin.text.buildString
import kotlin.text.isEmpty

object NeteaseUtils {
    private val HEX_CHARS = "0123456789abcdef"

    // 对应 Node: generateRequestId()
    fun generateRequestId(): String {
        val timestamp = System.currentTimeMillis()
        val randomInt = (Math.random() * 1000).toInt().toString().padStart(4, '0')
        return "${timestamp}_$randomInt"
    }

    // 对应 Node: CryptoJS.lib.WordArray.random(32).toString()
    // 生成 ntes_nuid, ntes_nnid 等
    fun getRandomHex(length: Int = 16) = buildString(length) {
        repeat(16) {
            append(HEX_CHARS.random())
        }
    }

    // 对应 Node: generateRandomChineseIP()
    // 简易版，生成一个常见的国内 IP 段，避免部分歌曲因版权/地区限制无法播放
    fun getRandomChineseIp(): String {
        val first = listOf(116, 119, 218, 220, 120).random()
        val second = (60..250).random()
        val third = (0..255).random()
        val fourth = (0..255).random()
        return "$first.$second.$third.$fourth"
    }

    fun chooseUserAgent(crypto: String, os: String): String {
        return when (crypto) {
            "weapi" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
            "linuxapi" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.90 Safari/537.36"
            else -> when (os) {
                "android" -> AndroidUserAgent
                "iphone" -> "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
                else -> "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164 NeteaseMusicDesktop/3.0.18.203152"
            }
        }
    }

    fun getAndroidId(): String = Base64.encode(
        "null\t${generateRandomMac()}\t${getRandomHex()}\t${getRandomHex()}".toByteArray(),
        url = true
    )

    fun getResourceLink(id: String, extension: String = "jpg"): String {
        val encrypted = encryptId(id)

        val seed = id.last().digitToInt()
        val p = when {
            seed < 2.5 -> 1
            seed < 5 -> 2
            seed < 7.5 -> 3
            else -> 4
        }

        return "http://p$p.music.126.net/$encrypted/$id.$extension"
    }

    suspend fun getAndroidId(context: Context): String {
        val androidId = context.dataStore[AndroidIdKey] ?: ""
        if (androidId.isEmpty()) {
            val androidId = Base64.encode(
                "null\t${generateRandomMac()}\t${getRandomHex()}\t${getRandomHex()}".toByteArray(),
                url = true
            )
            context.dataStore.edit {
                it[AndroidIdKey] = androidId
            }
            return androidId
        } else {
            return androidId
        }
    }

    fun getWNMCID(): String {
        val characters = "abcdefghijklmnopqrstuvwxyz"
        val randomString = (1..6).map { characters.random() }.joinToString("")
        return "$randomString.${System.currentTimeMillis()}.01.0"
    }



}

data class OSInfo(
    val os: String,
    val appver: String,
    val osver: String,
    val channel: String
)

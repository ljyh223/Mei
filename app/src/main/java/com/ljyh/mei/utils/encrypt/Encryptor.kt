package com.ljyh.mei.utils.encrypt

import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AndroidIdKey
import com.ljyh.mei.utils.dataStore
import com.ljyh.mei.utils.get
import korlibs.crypto.AES
import korlibs.crypto.Padding
import korlibs.crypto.md5
import korlibs.encoding.Base64
import korlibs.encoding.hex
import korlibs.encoding.toBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale

private const val presetKey = "0CoJUm6Qyw8W8jud"
private const val iv = "0102030405060708"
private const val eapiKey = "e82ckenh8dichen8"
private val CHARS = "0123456789ABCDEF"
private val HEX_CHARS = "0123456789abcdef"

/**
 * weapi 接口加密
 *
 * @param data 原始post请求数据
 * @return 加密后的post body
 */
fun encryptWeAPI(
    data: String
): WeApi {
    val key = createRandomKey()

    return WeApi(
        params = AES.encryptAesCbc(
            data = AES.encryptAesCbc(
                data = data.toByteArray(),
                key = presetKey.toByteArray(),
                iv = iv.toByteArray(),
                padding = Padding.PKCS7Padding
            ).toBase64().toByteArray(),
            key = key.toByteArray(),
            iv = iv.toByteArray(),
            padding = Padding.PKCS7Padding
        ).toBase64(),
        encSecKey = rsaEncrypt(
            key
        )

    )
}

fun decryptEApi(
    data: ByteArray
): String {

    return AES.decryptAesEcb(
        data = data,
        key = eapiKey.toByteArray(),
        padding = Padding.PKCS7Padding
    ).decodeToString()
}

fun encryptEApi(
    url: String,
    data: String
): EApi {
    Log.d("Eapi", "data: $data")
    Log.d("Eapi", "url: $url")
    val message = "nobody" + url + "use" + data + "md5forencrypt"
    val digest: String = message.toByteArray().md5().hex
    return EApi(
        params = AES.encryptAesEcb(
            data = "$url-36cd479b6b5-$data-36cd479b6b5-$digest".toByteArray(),
            key = eapiKey.toByteArray(),
            padding = Padding.PKCS7Padding
        ).hex
    )
}

fun createRandomKey(length: Int = 16) = StringBuilder().apply {
    repeat(length) {
        append((('a'..'z') + ('A'..'Z') + ('0'..'9')).random())
    }
}.toString()


fun encryptId(id: String): String {
    val keyBytes = "3go8&$8*3*3h0k(2)2".toByteArray()
    val idBytes = id.toByteArray()

    val xored = ByteArray(idBytes.size)
    for (i in idBytes.indices) {
        xored[i] = (idBytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
    }
    return xored.md5().base64.replace("/", "_").replace("+", "-")

}


fun generateRandomMac(): String {
    val parts = Array(6) { i ->
        val high = CHARS.random()
        val low = CHARS.random()
        "$high$low"
    }
    val firstByte = parts[0].toInt(16) and 0xFE
    parts[0] = firstByte.toString(16).uppercase().padStart(2, '0')

    return parts.joinToString(":")
}

fun generateRandomAndroidId(): String = buildString(16) {
    repeat(16) {
        append(HEX_CHARS.random())
    }
}

suspend fun getAndroidId(context: Context): String {
    val androidId = context.dataStore[AndroidIdKey] ?: ""
    if (androidId.isEmpty()) {
        val androidId = Base64.encode(
            "null\t${generateRandomMac()}\t${generateRandomAndroidId()}\t${generateRandomAndroidId()}".toByteArray(),
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

fun getAndroidId() :String= Base64.encode(
    "null\t${generateRandomMac()}\t${generateRandomAndroidId()}\t${generateRandomAndroidId()}".toByteArray(),
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

data class WeApi(
    val params: String,
    val encSecKey: String
)

data class EApi(
    val params: String,
)
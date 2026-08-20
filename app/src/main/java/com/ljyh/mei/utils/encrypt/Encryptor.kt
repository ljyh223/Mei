package com.ljyh.mei.utils.encrypt

import com.google.gson.JsonParser
import korlibs.crypto.AES
import korlibs.crypto.Padding
import korlibs.crypto.md5
import korlibs.encoding.hex
import korlibs.encoding.toBase64
import timber.log.Timber

private const val presetKey = "0CoJUm6Qyw8W8jud"
private const val iv = "0102030405060708"
private const val eapiKey = "e82ckenh8dichen8"
private val CHARS = "0123456789ABCDEF"


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
    require(data.isNotEmpty() && data.size % AES_BLOCK_SIZE == 0) {
        "Invalid EAPI ciphertext length: ${data.size}"
    }
    return AES.decryptAesEcb(
        data = data,
        key = eapiKey.toByteArray(),
        padding = Padding.PKCS7Padding
    ).decodeToString()
}

fun decodeEApiResponse(data: ByteArray): String {
    data.plainJsonOrNull()?.let { return it }
    return decryptEApi(data)
}

private fun ByteArray.plainJsonOrNull(): String? {
    var index = 0
    if (
        size >= UTF8_BOM.size &&
        UTF8_BOM.indices.all { bomIndex -> this[bomIndex] == UTF8_BOM[bomIndex] }
    ) {
        index = UTF8_BOM.size
    }
    while (index < size && this[index].toInt().toChar().isWhitespace()) index++
    if (index >= size || (this[index] != JSON_OBJECT_START && this[index] != JSON_ARRAY_START)) {
        return null
    }

    val candidate = decodeToString(startIndex = index)
    return runCatching {
        JsonParser.parseString(candidate)
            .takeIf { it.isJsonObject || it.isJsonArray }
            ?.let { candidate }
    }.getOrNull()
}

fun encryptEApi(
    url: String,
    data: String
): EApi {
    Timber.tag("Eapi").d( "data: $data")
    Timber.tag("Eapi").d("url: $url")
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




data class WeApi(
    val params: String,
    val encSecKey: String
)

data class EApi(
    val params: String,
)

private const val AES_BLOCK_SIZE = 16
private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
private val JSON_OBJECT_START = '{'.code.toByte()
private val JSON_ARRAY_START = '['.code.toByte()

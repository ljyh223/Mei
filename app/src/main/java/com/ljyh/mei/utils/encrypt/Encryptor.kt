package com.ljyh.mei.utils.encrypt

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val presetKey = "0CoJUm6Qyw8W8jud"
private const val weApiIv = "0102030405060708"
private const val eapiKey = "e82ckenh8dichen8"
private val CHARS = "0123456789ABCDEF"


/**
 * weapi 接口加密
 *
 * @param data 原始post请求数据
 * @return 加密后的post body
 */
fun encryptWeAPI(data: String): WeApi = encryptWeApiWithKey(data, createRandomKey())

internal fun encryptWeApiWithKey(data: String, key: String): WeApi =
    WeApi(
        params = Base64.getEncoder().encodeToString(
            aesCbcEncrypt(
                key.toByteArray(),
                Base64.getEncoder().encode(aesCbcEncrypt(presetKey.toByteArray(), data.toByteArray()))
            )
        ),
        encSecKey = rsaEncrypt(key)
    )

fun decryptEApi(
    data: ByteArray
): String {
    require(data.isNotEmpty() && data.size % AES_BLOCK_SIZE == 0) {
        "Invalid EAPI ciphertext length: ${data.size}"
    }
    val decrypted = aesEcbCipher(Cipher.DECRYPT_MODE, data)
    val payload = if (decrypted.size >= 2 &&
        decrypted[0] == 0x1f.toByte() && decrypted[1] == 0x8b.toByte()
    ) {
        GZIPInputStream(ByteArrayInputStream(decrypted)).use { it.readBytes() }
    } else {
        decrypted
    }
    return payload.decodeToString()
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
        Json.parseToJsonElement(candidate)
            .takeIf { it is JsonObject || it is JsonArray }
            ?.let { candidate }
    }.getOrNull()
}

fun encryptEApi(
    url: String,
    data: String
): EApi {
    val message = "nobody" + url + "use" + data + "md5forencrypt"
    val digest = MessageDigest.getInstance("MD5").digest(message.toByteArray()).toLowerHex()
    return EApi(
        params = aesEcbCipher(
            Cipher.ENCRYPT_MODE,
            "$url-36cd479b6b5-$data-36cd479b6b5-$digest".toByteArray()
        ).toLowerHex()
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
    return Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(xored))
        .replace("/", "_").replace("+", "-")

}

data class WeApi(
    val params: String,
    val encSecKey: String
)

data class EApi(
    val params: String,
)

private const val AES_BLOCK_SIZE = 16
private val HEX_DIGITS = "0123456789abcdef"
private fun ByteArray.toLowerHex(): String = buildString(size * 2) {
    for (byte in this@toLowerHex) {
        val value = byte.toInt() and 0xff
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0f])
    }
}

private fun aesCbcEncrypt(key: ByteArray, data: ByteArray): ByteArray =
    Cipher.getInstance("AES/CBC/PKCS5Padding").run {
        init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(weApiIv.toByteArray()))
        doFinal(data)
    }

private fun aesEcbCipher(mode: Int, data: ByteArray): ByteArray =
    Cipher.getInstance("AES/ECB/PKCS5Padding").run {
        init(mode, SecretKeySpec(eapiKey.toByteArray(), "AES"))
        doFinal(data)
    }

private val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
private val JSON_OBJECT_START = '{'.code.toByte()
private val JSON_ARRAY_START = '['.code.toByte()

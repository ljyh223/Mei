package com.ljyh.mei.utils.encrypt

import android.util.Log
import korlibs.crypto.AES
import korlibs.crypto.Padding
import korlibs.crypto.md5
import korlibs.encoding.hex
import korlibs.encoding.toBase64

private const val presetKey = "0CoJUm6Qyw8W8jud"
private const val iv = "0102030405060708"
private const val eapiKey = "e82ckenh8dichen8"

/**
 * weapi 接口加密
 *
 * @param data 原始post请求数据
 * @return 加密后的post body
 */
fun encryptWeAPI(
    data: String
) : WeApi {
    val key = createRandomKey()

    return WeApi(
        params =   AES.encryptAesCbc(
            data = AES.encryptAesCbc(
                data = data.toByteArray(),
                key = presetKey.toByteArray(),
                iv = iv.toByteArray(),
                padding = Padding.PKCS7Padding
            ).toBase64().toByteArray(),
            key= key.toByteArray(),
            iv = iv.toByteArray(),
            padding = Padding.PKCS7Padding
        ).toBase64(),
         encSecKey =  rsaEncrypt(
            key
        )

    )
}

fun decryptEApi(
    data: ByteArray
) : String {

    return AES.decryptAesEcb(
        data = data,
        key = eapiKey.toByteArray(),
        padding = Padding.PKCS7Padding
    ).decodeToString()
}

fun encryptEApi(
    url: String,
    data: String
) : EApi {
    Log.d("Eapi", "data: $data")
    Log.d("Eapi", "url: $url")
    val message = "nobody" + url + "use" + data + "md5forencrypt"
    val digest: String = message.toByteArray().md5().hex
    return EApi(
        params =  AES.encryptAesEcb(
            data = "$url-36cd479b6b5-$data-36cd479b6b5-$digest".toByteArray(),
            key = eapiKey.toByteArray(),
            padding = Padding.PKCS7Padding
        ).hex
    )
}

fun createRandomKey(length: Int = 16) = StringBuilder().apply {
    repeat(length){
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
    return  xored.md5().base64.replace("/", "_").replace("+", "-")

}




fun getResourceLink(id: String, extension: String= "jpg"): String {
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
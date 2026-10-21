package com.ljyh.mei.playback.source.lx

import kotlinx.serialization.json.*
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Small explicit bridge; scripts never receive a Java object or reflection access. */
internal object LxCrypto {
    fun invoke(raw: String): String {
        require(raw.length <= 2 * 1024 * 1024)
        val obj = lxJson.parseToJsonElement(raw).jsonObject
        fun bytes(key: String): ByteArray = (obj[key] as? JsonArray)?.map { it.jsonPrimitive.int.toByte() }?.toByteArray()
            ?: obj.string(key).toByteArray(Charsets.UTF_8)
        fun array(value: ByteArray): String = JsonArray(value.map { JsonPrimitive(it.toInt() and 255) }).toString()
        return when (obj.string("op")) {
            "from" -> array(when (obj.string("format", "utf8")) {
                "base64" -> Base64.getDecoder().decode(obj.string("value"))
                "hex" -> obj.string("value").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                "utf8", "utf-8" -> obj.string("value").toByteArray(Charsets.UTF_8)
                else -> error("Unsupported encoding")
            })
            "string" -> JsonPrimitive(when (obj.string("format", "utf8")) {
                "base64" -> Base64.getEncoder().encodeToString(bytes("value"))
                "hex" -> bytes("value").joinToString("") { "%02x".format(it.toInt() and 255) }
                "utf8", "utf-8" -> bytes("value").toString(Charsets.UTF_8)
                else -> error("Unsupported encoding")
            }).toString()
            "md5" -> JsonPrimitive(MessageDigest.getInstance("MD5").digest(bytes("value"))
                .joinToString("") { "%02x".format(it.toInt() and 255) }).toString()
            "random" -> array(ByteArray(obj.long("size").toInt().also { require(it in 0..65_536) }).also { SecureRandom().nextBytes(it) })
            "aes" -> {
                val mode = obj.string("mode")
                require(mode in setOf("aes-128-cbc", "aes-128-ecb"))
                val cipher = Cipher.getInstance(if (mode.endsWith("cbc")) "AES/CBC/PKCS5Padding" else "AES/ECB/PKCS5Padding")
                val key = SecretKeySpec(bytes("key").also { require(it.size == 16) }, "AES")
                if (mode.endsWith("cbc")) cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(bytes("iv")))
                else cipher.init(Cipher.ENCRYPT_MODE, key)
                array(cipher.doFinal(bytes("value")))
            }
            "rsa" -> {
                val pem = obj.string("key").replace(Regex("-----[^-]+-----|\\s"), "")
                val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(pem)))
                val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                array(cipher.doFinal(bytes("value")))
            }
            else -> error("Unsupported utility")
        }
    }
}

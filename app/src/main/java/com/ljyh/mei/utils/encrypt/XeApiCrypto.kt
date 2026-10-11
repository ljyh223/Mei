package com.ljyh.mei.utils.encrypt

import com.google.crypto.tink.subtle.X25519
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class XeApiPublicKey(
    val publicKey: String,
    val version: String,
    val sk: String,
    val nextUpdateTime: Long,
)

data class XeApiSession(val id: String, val key: String)

/** 9.6.05 C/S/R envelope. The public key and session are supplied by XeApiClient. */
class XeApiCrypto(private val random: SecureRandom = SecureRandom()) {
    fun encrypt(
        encodedQuery: String,
        publicKey: XeApiPublicKey,
        session: XeApiSession? = null,
        encodedContent: String = "",
    ): String {
        val peerKey = Base64.getDecoder().decode(publicKey.publicKey)
        require(peerKey.size == 32) { "Invalid XEAPI public key" }
        val dynamicKey = session?.key?.toByteArray(StandardCharsets.UTF_8) ?: randomBytes(16)
        require(dynamicKey.size in setOf(16, 24, 32)) { "Invalid XEAPI session key" }

        val plaintext = buildJsonObject {
            if (encodedQuery.isNotEmpty()) put("queryString", "$encodedQuery&e_r=true")
            put("content", encodedContent)
            if (encodedQuery.isEmpty()) put("queryString", "e_r=true")
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val staticCiphertext = aesEcbEncrypt(STATIC_KEY, plaintext)
        val prefix = randomBytes(16)
        val xored = ByteArray(staticCiphertext.size) { index ->
            (staticCiphertext[index].toInt() xor prefix[index and 15].toInt()).toByte()
        }
        val offset = (prefix[0].toInt() and 15) % xored.size
        val transformed = prefix + xored.copyOfRange(offset, xored.size) +
            xored.copyOfRange(0, offset)
        val c = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(aesEcbEncrypt(dynamicKey, transformed))

        val privateKey = X25519.generatePrivateKey()
        val ephemeralKey = X25519.publicFromPrivate(privateKey)
        val sharedSecret = X25519.computeSharedSecret(privateKey, peerKey)
        val prk = hmacSha256(ByteArray(32), sharedSecret)
        val aesKey = hmacSha256(prk, ephemeralKey + byteArrayOf(1)).copyOf(16)
        val iv = randomBytes(12)
        val gcm = Cipher.getInstance("AES/GCM/NoPadding")
        gcm.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        val sPayload = "${Base64.getEncoder().encodeToString(dynamicKey)}|android|${publicKey.sk}"
            .toByteArray(StandardCharsets.UTF_8)
        val s = Base64.getEncoder().encodeToString(
            ephemeralKey + iv + gcm.doFinal(sPayload)
        )
        val r = Base64.getEncoder().encodeToString(
            aesEcbEncrypt(STATIC_KEY, "${publicKey.version}|${session?.id.orEmpty()}".toByteArray())
        )
        return "C=$c&S=${formEncode(s)}&R=${formEncode(r)}"
    }

    fun decryptPublicKey(encryptedData: String): String =
        aesEcbDecrypt(STATIC_KEY, Base64.getDecoder().decode(encryptedData))
            .toString(StandardCharsets.UTF_8)

    fun sign(timestamp: String, nonce: String): String = Base64.getEncoder().encodeToString(
        hmacSha256(SIGN_KEY.toByteArray(StandardCharsets.UTF_8),
            (timestamp + nonce).toByteArray(StandardCharsets.UTF_8))
    )

    fun nonce(): String = buildString(16) { repeat(16) { append(random.nextInt(10)) } }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also(random::nextBytes)

    private fun formEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun hmacSha256(key: ByteArray, input: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256"))
            doFinal(input)
        }

    private fun aesEcbEncrypt(key: ByteArray, input: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
            doFinal(input)
        }

    private fun aesEcbDecrypt(key: ByteArray, input: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
            doFinal(input)
        }

    private companion object {
        val STATIC_KEY = hexBytes("ab1d5a430f6bb04a3f01e81ddd72bd916d5ce591248ac128714806d7f8fb1b84")
        const val SIGN_KEY = "mUHCwVNWJbunMqAHf5MImuirT6plvs6VSFW62MGHstFQxhBGdEoIhLItH3djc4+FB/OKty3+lL2rGeoFBpVe5g=="

        fun hexBytes(hex: String): ByteArray = ByteArray(hex.length / 2) { index ->
            hex.substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }
}

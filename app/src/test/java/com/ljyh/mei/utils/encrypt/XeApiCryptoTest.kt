package com.ljyh.mei.utils.encrypt

import com.google.crypto.tink.subtle.X25519
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XeApiCryptoTest {
    @Test
    fun publicKeyRegistrationSignatureMatchesReference() {
        assertEquals(
            "jvwzjt14T8zBQFmh2GnSTcZZXXZnf56Dd6KWQ/DhVMQ=",
            XeApiCrypto().sign("1234567890123", "0123456789012345"),
        )
    }

    @Test
    fun csrEnvelopeMatchesCapturedShapeAndRoundTripsBusinessPlaintext() {
        val serverPrivate = X25519.generatePrivateKey()
        val key = XeApiPublicKey(
            publicKey = Base64.getEncoder().encodeToString(X25519.publicFromPrivate(serverPrivate)),
            version = "1000000000000",
            sk = "FCSGJukj1781616326176",
            nextUpdateTime = Long.MAX_VALUE,
        )
        val session = XeApiSession("s".repeat(32), "k".repeat(32))
        val query = "https://interface3.music.163.com/xeapi/song/enhance/player/url/v1"
            .toHttpUrl().newBuilder()
            .addQueryParameter("ids", "[\"393594_0\",\"409647155_0\"]")
            .addQueryParameter("level", "exhigh")
            .addQueryParameter("encodeType", "aac")
            .addQueryParameter("trialMode", "1")
            .addQueryParameter("faql", "0")
            .addQueryParameter("immerseType", "ste2")
            .addQueryParameter("degradeImmerseType", "aac")
            .addQueryParameter("expParams", "{\"hy-zyyinzhi\":\"t1\"}")
            .addQueryParameter("cliUserId", "1234567890")
            .addQueryParameter("cliVipTypes", "[1,6,15]")
            .addQueryParameter("trialModes", "{\"393594\":1,\"409647155\":1}")
            .addQueryParameter("supportDolby", "false")
            .addQueryParameter("volumeBalance", "0")
            .addQueryParameter("djVolumeBalance", "1")
            .build().encodedQuery!!

        val form = XeApiCrypto().encrypt(query, key, session).split('&').associate { pair ->
            val (name, value) = pair.split('=', limit = 2)
            name to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
        assertEquals(setOf("C", "S", "R"), form.keys)
        assertEquals(555, form.getValue("C").length)
        assertEquals(180, form.getValue("S").length)
        assertEquals(64, form.getValue("R").length)
        assertTrue(form.getValue("C").matches(Regex("[A-Za-z0-9_-]+")))

        val plaintext = decryptC(form.getValue("C"), session.key.toByteArray())
        assertEquals("{\"queryString\":\"$query&e_r=true\",\"content\":\"\"}", plaintext)

        val r = aesDecrypt(STATIC_KEY, Base64.getDecoder().decode(form.getValue("R")))
        assertEquals("${key.version}|${session.id}", r.toString(StandardCharsets.UTF_8))

        val s = Base64.getDecoder().decode(form.getValue("S"))
        val ephemeral = s.copyOfRange(0, 32)
        val iv = s.copyOfRange(32, 44)
        val prk = hmac(ByteArray(32), X25519.computeSharedSecret(serverPrivate, ephemeral))
        val aesKey = hmac(prk, ephemeral + byteArrayOf(1)).copyOf(16)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, iv))
        val sPlaintext = cipher.doFinal(s.copyOfRange(44, s.size)).toString(StandardCharsets.UTF_8)
        assertEquals("${Base64.getEncoder().encodeToString(session.key.toByteArray())}|android|${key.sk}", sPlaintext)

        val firstRequest = XeApiCrypto().encrypt(query, key).split('&').associate { pair ->
            val (name, value) = pair.split('=', limit = 2)
            name to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
        assertEquals(555, firstRequest.getValue("C").length)
        assertEquals(152, firstRequest.getValue("S").length)
        assertEquals(24, firstRequest.getValue("R").length)
    }

    @Test
    fun csrWithoutQueryEncryptsFormContentAndOnlyERInQueryString() {
        val serverPrivate = X25519.generatePrivateKey()
        val publicKey = XeApiPublicKey(
            publicKey = Base64.getEncoder().encodeToString(X25519.publicFromPrivate(serverPrivate)),
            version = "1000000000000",
            sk = "test-sk",
            nextUpdateTime = Long.MAX_VALUE,
        )
        val encodedContent = "scene=playlist_head&playlistId=123&newStyle=true"
        val session = XeApiSession("s".repeat(32), "k".repeat(32))
        val c = XeApiCrypto().encrypt("", publicKey, session, encodedContent)
            .substringAfter("C=").substringBefore('&')
        assertEquals(
            "{\"content\":\"$encodedContent\",\"queryString\":\"e_r=true\"}",
            decryptC(c, session.key.toByteArray()),
        )
        val emptyContentC = XeApiCrypto().encrypt("", publicKey, session)
            .substringAfter("C=").substringBefore('&')
        assertEquals(
            "{\"content\":\"\",\"queryString\":\"e_r=true\"}",
            decryptC(emptyContentC, session.key.toByteArray()),
        )
    }

    private fun decryptC(c: String, dynamicKey: ByteArray): String {
        val outer = aesDecrypt(dynamicKey, Base64.getUrlDecoder().decode(c))
        val prefix = outer.copyOfRange(0, 16)
        val rotated = outer.copyOfRange(16, outer.size)
        val offset = (prefix[0].toInt() and 15) % rotated.size
        val xored = rotated.copyOfRange(rotated.size - offset, rotated.size) +
            rotated.copyOfRange(0, rotated.size - offset)
        val staticCiphertext = ByteArray(xored.size) { index ->
            (xored[index].toInt() xor prefix[index and 15].toInt()).toByte()
        }
        return aesDecrypt(STATIC_KEY, staticCiphertext).toString(StandardCharsets.UTF_8)
    }

    private fun aesDecrypt(key: ByteArray, data: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
            doFinal(data)
        }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
        init(SecretKeySpec(key, "HmacSHA256"))
        doFinal(data)
    }

    private companion object {
        val STATIC_KEY = "ab1d5a430f6bb04a3f01e81ddd72bd916d5ce591248ac128714806d7f8fb1b84"
            .chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }
}

package com.ljyh.mei.utils.encrypt

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class EApiResponseDecoderTest {
    @Test
    fun decryptsGzippedEapiResponse() {
        val response = "{\"code\":200,\"data\":[1,2]}"
        val compressed = ByteArrayOutputStream().also { stream ->
            GZIPOutputStream(stream).use { it.write(response.toByteArray()) }
        }.toByteArray()
        val encrypted = Cipher.getInstance("AES/ECB/PKCS5Padding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec("e82ckenh8dichen8".toByteArray(), "AES"))
            doFinal(compressed)
        }

        assertEquals(response, decodeEApiResponse(encrypted))
    }

    @Test
    fun plainJsonObjectPassesThroughWithoutAesDecryption() {
        val response = "  {\"code\":200,\"data\":{\"playDuration\":874}}"

        assertEquals(
            "{\"code\":200,\"data\":{\"playDuration\":874}}",
            decodeEApiResponse(response.toByteArray()),
        )
    }

    @Test
    fun jsonArrayWithUtf8BomPassesThrough() {
        val response = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) +
            "[1,2,3]".toByteArray()

        assertEquals("[1,2,3]", decodeEApiResponse(response))
    }

    @Test
    fun ciphertextLikePayloadStartingWithJsonMarkerIsNotTreatedAsPlainJson() {
        val response = ByteArray(18) { 1 }.also { it[0] = '{'.code.toByte() }

        try {
            decodeEApiResponse(response)
            fail("Expected invalid EAPI ciphertext length")
        } catch (error: IllegalArgumentException) {
            assertEquals("Invalid EAPI ciphertext length: 18", error.message)
        }
    }

    @Test
    fun invalidCiphertextLengthFailsBeforeCipherReadsPastTheBuffer() {
        try {
            decodeEApiResponse(ByteArray(18) { 1 })
            fail("Expected invalid EAPI ciphertext length")
        } catch (error: IllegalArgumentException) {
            assertEquals("Invalid EAPI ciphertext length: 18", error.message)
        }
    }
}

package com.ljyh.mei.utils.encrypt

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class EApiResponseDecoderTest {
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

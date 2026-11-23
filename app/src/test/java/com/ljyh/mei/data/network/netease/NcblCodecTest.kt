package com.ljyh.mei.data.network.netease

import com.github.luben.zstd.Zstd
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import org.junit.Assert.*
import org.junit.Test

class NcblCodecTest {
    @Test fun chachaMatchesRfc8439EncryptionVector() {
        val plain = "Ladies and Gentlemen of the class of '99: If I could offer you only one tip for the future, sunscreen would be it.".toByteArray()
        val cipher = hex("6e2e359a2568f98041ba0728dd0d6981e97e7aec1d4360c20a27afccfd9fae0bf91b65c5524733ab8f593dabcd62b3571639d624e65152ab8f530c359f0861d807ca0dbf500d6a6156a38e088a22b65e52bc514d16ccf806818ce91ab77937365af90bbf74a35be6b40b8eedf2785e42874d")
        assertArrayEquals(cipher, NcblCodec.chacha20(ByteArray(32) { it.toByte() }, hex("000000000000004a00000000"), 1, plain))
    }

    @Test fun envelopeMatchesIndependentPythonCryptographyAndLibzstdFixture() {
        // Independently generated with Python cryptography + libzstd level 3 and the official layout.
        val fixture = hex("4e43424c0300000066002021222324254627a8292a2b2c2d2e2fdda20437ce173c34273cb03bffb85db8f3ce53bc2f1334a752303d26890094af30310000303100003500000043431c00702067c6b550dffa824c44575851bc8f6f8fd9ddaf91b0588acb31732f00303100003561fc93cc91f8da728fcf5c4ebe4ff176e60d363182fa9cc0780cc6510825b5d15e5c5069d652d6fcca3cc3a74e39")
        val encoded = NcblCodec.encode(
            "{\"client\":\"Mei\",\"version\":3}".toByteArray(),
            "1700000000\u0001_plv\u0001{\"id\":12345,\"time\":0}\n".toByteArray(),
            ByteArray(50) { it.toByte() },
        )
        assertArrayEquals(fixture, encoded)
    }

    @Test fun largeIncompressiblePayloadPreservesEveryFrameAndRetryIdentity() {
        val raw = ByteArray(90000).also(SecureRandom()::nextBytes)
        val entropy = NcblCodec.entropy()
        val encoded = NcblCodec.encode("{}".toByteArray(), raw, entropy)
        assertArrayEquals(encoded, NcblCodec.encode("{}".toByteArray(), raw, entropy))
        val view = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN)
        val uuid = encoded.copyOfRange(10, 26)
        val nonce = uuid.copyOfRange(0, 12)
        val counter = ByteBuffer.wrap(uuid).order(ByteOrder.LITTLE_ENDIAN).getInt(12) ushr 2
        val key = entropy.copyOfRange(0, 32).apply { if ((this[0].toInt() and 255) >= 0xa3) this[0] = 0xa2.toByte() }
        var offset = view.getShort(8).toInt() and 65535
        var seq = view.getInt(58)
        val compressed = java.io.ByteArrayOutputStream()
        var count = 0
        while (offset < encoded.size) {
            val length = view.getShort(offset).toInt() and 65535
            assertTrue(length <= 32768)
            assertEquals(seq++, view.getInt(offset + 2))
            compressed.write(NcblCodec.chacha20(key, nonce, counter, encoded.copyOfRange(offset + 6, offset + 6 + length)))
            offset += length + 6
            count++
        }
        assertTrue(count >= 3)
        assertEquals(view.getInt(62), seq - 1)
        assertArrayEquals(raw, Zstd.decompress(compressed.toByteArray(), raw.size))
    }

    private fun hex(value: String) = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

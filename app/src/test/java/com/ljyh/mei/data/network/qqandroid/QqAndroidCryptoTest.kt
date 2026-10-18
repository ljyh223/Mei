package com.ljyh.mei.data.network.qqandroid

import com.ljyh.unblockneteasemusic.qq.QRCUtils
import java.util.Base64
import java.util.zip.Inflater
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * QQ 音乐 Android 协议的纯计算回归测试。
 *
 * 期望值由 tools/qqmusic-signature-analysis 里的 Python 参考实现生成
 * （同一套算法已对真实抓包逐字节验证过），这里只做算法一致性回归，
 * 不含任何设备标识。
 */
class QqAndroidCryptoTest {

    private val content =
        """{"comm":{"ct":"11"},"music.test.M.m":{"module":"music.test.M","method":"m","param":{"a":1}}}"""
            .toByteArray(Charsets.UTF_8)

    @Test
    fun `sign matches reference implementation`() {
        val nonce = ByteArray(12) { it.toByte() }
        // Python: hmac_sha1(key=b"9FF169D646A3", reverse(base64(content)))
        assertEquals(
            "AAECAwQFBgcICQoLW9Gs1gP7PMka6J4kxUOcaLrJePQ=",
            QqAndroidCrypto.sign(content, nonce),
        )
    }

    @Test
    fun `tea cbc matches reference implementation`() {
        val key = "0123456789abcdef".toByteArray(Charsets.US_ASCII)
        val padded = ByteArray(40) { (it + 1).toByte() }
        val expected =
            "80ca512e81fdc23df8d151bece1639b47c90a5f4b0d26568f3600f18750c4d11d600e5f6f4991a57"

        val encrypted = QqAndroidCrypto.teaCbcEncrypt(padded, key)
        assertEquals(expected, encrypted.joinToString("") { "%02x".format(it) })
        assertArrayEquals(padded, QqAndroidCrypto.teaCbcDecrypt(encrypted, key))
    }

    @Test
    fun `mask round trips back to custom`() {
        val sign = "RExETUFKWlhXWVVBwWKZ7gVm6Ev030OiyOZTMv5OX1w="
        val custom = "11&20090008&1234567890&null&1700000000000&abcdef&android"
        val key = (sign.take(8) + sign.takeLast(8)).toByteArray(Charsets.US_ASCII)

        val mask = QqAndroidCrypto.mask(sign, custom)
        val padded = QqAndroidCrypto.teaCbcDecrypt(Base64.getDecoder().decode(mask), key)
        val prefixLen = padded[0].toInt() and 0x07
        val plain = padded.copyOfRange(1 + prefixLen + 2, padded.size)

        assertEquals(custom, plain.toString(Charsets.UTF_8).trimEnd('\u0000'))
    }

    @Test
    fun `m encoding is five random bytes plus deflate`() {
        val encoded = QqAndroidCrypto.mEncode(content)
        assertTrue("前缀应为 5 字节", encoded.size > 5)
        assertTrue("前缀取值 0..99", encoded.take(5).all { it.toInt() in 0..99 })

        val inflater = Inflater()
        inflater.setInput(encoded.copyOfRange(5, encoded.size))
        val out = ByteArray(content.size + 16)
        val n = inflater.inflate(out)
        inflater.end()
        assertArrayEquals(content, out.copyOfRange(0, n))
    }

    @Test
    fun `m value decrypts to empty json object`() {
        val aid = "0123456789abcdef"
        val mask = QqAndroidCrypto.mValue(aid)
        val padded = QqAndroidCrypto.teaCbcDecrypt(Base64.getDecoder().decode(mask), aid.toByteArray())
        val prefixLen = padded[0].toInt() and 0x07
        val plain = padded.copyOfRange(1 + prefixLen + 2, padded.size)
        assertEquals("{}", plain.toString(Charsets.UTF_8).trimEnd('\u0000'))
    }

    @Test
    fun `qrc decoder matches reference vector`() {
        // Python: DES(KEY3) -> DES^-1(KEY2) -> DES(KEY1) 作用于 zlib 压缩过的 XML
        val hex =
            "0c8d67dd3e549974b64ed2680459f13881aa15d10db4cc8324b86311d0d741bd6af5d8724f2b7571b2b2bf976" +
                "be395e49ce543e87bf085b0d05bc5f45ffab18469573df64df0d8855de3d9eace217cfd54e8c1c3956dde2449" +
                "27e5abf973b3bd301f99a4d7db3bc66ab549dcceba94d7520844d17396542f7c7bcc5f1a3d250af6f58ec7126" +
                "7a1bcb7ed1d3a95d5dbe0"
        assertEquals("[0,100]abc(0,100)def", QRCUtils.decodeLyric(hex))
    }
}

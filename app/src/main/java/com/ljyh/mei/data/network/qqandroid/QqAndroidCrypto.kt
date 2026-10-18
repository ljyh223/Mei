package com.ljyh.mei.data.network.qqandroid

import java.security.SecureRandom
import java.util.Base64
import java.util.zip.Deflater
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * QQ 音乐 **Android 客户端协议**（`u6.y.qq.com/cgi-bin/musics.fcg`）的签名/加密原语。
 *
 * 注意区分项目里另外两条链路，它们和这里**不是**一回事：
 *  - [com.ljyh.mei.data.network.XeApiClient]  —— 网易云 XEAPI（C/S/R 加密）
 *  - [com.ljyh.unblockneteasemusic.qq.QqMusicProvider] —— QQ 旧版 `musicu.fcg`（不需要 sign/mask，走 cookie）
 *
 * 本文件只做纯计算，不依赖 Android，方便在 JVM 单测里对着抓包做回归。
 */
object QqAndroidCrypto {

    /** 12 字节 HMAC 密钥（libmer.so 里由 6 个混淆 dword 异或拼出）。 */
    const val HMAC_KEY = "9FF169D646A3"

    private const val DELTA = -1640531527 // 0x9E3779B9
    private const val ROUNDS = 16
    private val random = SecureRandom()

    // ------------------------------------------------------------------ sign
    /**
     * sign = base64( nonce[12] || HMAC-SHA1(key, reverse(base64(content))) )
     *
     * @param content 请求体**明文 JSON 字节**（不是 deflate 之后的那份）
     */
    fun sign(content: ByteArray, nonce: ByteArray = randomBytes(12)): String {
        val data = Base64.getEncoder().encodeToString(content).reversed().toByteArray(Charsets.US_ASCII)
        val mac = hmacSha1(HMAC_KEY.toByteArray(Charsets.US_ASCII), data)
        return Base64.getEncoder().encodeToString(nonce + mac)
    }

    // ------------------------------------------------------------------ mask
    /**
     * mask = base64( TEA_CBC(key = sign 前8 + 后8 字符, data = custom) )
     */
    fun mask(signBase64: String, custom: String): String {
        val key = (signBase64.take(8) + signBase64.takeLast(8)).toByteArray(Charsets.US_ASCII)
        return Base64.getEncoder().encodeToString(
            teaCbcEncrypt(teaPack(custom.toByteArray(Charsets.UTF_8)), key),
        )
    }

    // ------------------------------------------------------------------ M-Encoding
    /** M-Encoding：5 字节随机前缀（0..99）+ deflate。 */
    fun mEncode(content: ByteArray): ByteArray {
        val prefix = ByteArray(5) { random.nextInt(100).toByte() }
        return prefix + deflate(content)
    }

    /** M-Value = base64( TEA_CBC(key = androidId, "{}") )。 */
    fun mValue(androidId: String): String {
        val key = androidId.toByteArray(Charsets.UTF_8).copyOf(16)
        return Base64.getEncoder().encodeToString(
            teaCbcEncrypt(teaPack("{}".toByteArray(Charsets.UTF_8)), key),
        )
    }

    // ------------------------------------------------------------------ TEA-CBC（QQ 私有变体）
    /**
     * 明文布局：[1B 头][prefixLen B 随机][2B 随机][明文][7B 0x00]，
     * 头字节低 3 位 = prefixLen，高 5 位随机。
     */
    fun teaPack(plain: ByteArray, rng: SecureRandom = random): ByteArray {
        var prefixLen = (plain.size + 10) % 8
        if (prefixLen != 0) prefixLen = 8 - prefixLen
        val out = ByteArray(1 + prefixLen + 2 + plain.size + 7)
        out[0] = ((rng.nextInt(256) and 0xF8) or prefixLen).toByte()
        var p = 1
        repeat(prefixLen) { out[p++] = rng.nextInt(256).toByte() }
        repeat(2) { out[p++] = rng.nextInt(256).toByte() }
        plain.copyInto(out, p)
        return out
    }

    /** 去掉 [teaPack] 加的随机头，还原明文。 */
    fun teaUnpackPadded(padded: ByteArray, size: Int): ByteArray {
        val prefixLen = padded[0].toInt() and 0x07
        val start = 1 + prefixLen + 2
        return padded.copyOfRange(start, minOf(padded.size, start + size))
    }

    /**
     * C_i = TEA(P_i ^ C_{i-1}) ^ X_{i-1}，其中 X 是"上一轮异或后的输入"（首轮全 0）。
     */
    fun teaCbcEncrypt(padded: ByteArray, key16: ByteArray): ByteArray {
        val k = intKey(key16)
        val out = ByteArray(padded.size)
        var prevIn = ByteArray(8)
        var prevOut = ByteArray(8)
        var off = 0
        while (off < padded.size) {
            val x = ByteArray(8) { i -> (padded[off + i].toInt() xor prevOut[i].toInt()).toByte() }
            var (v0, v1) = readWords(x)
            val e = encryptBlock(v0, v1, k)
            val enc = ByteArray(8)
            writeWord(enc, 0, e.first)
            writeWord(enc, 4, e.second)
            for (i in 0 until 8) enc[i] = (enc[i].toInt() xor prevIn[i].toInt()).toByte()
            enc.copyInto(out, off)
            prevIn = x
            prevOut = enc
            off += 8
        }
        return out
    }

    fun teaCbcDecrypt(cipher: ByteArray, key16: ByteArray): ByteArray {
        val k = intKey(key16)
        val out = ByteArray(cipher.size)
        var prevIn = ByteArray(8)
        var prevOut = ByteArray(8)
        var off = 0
        while (off < cipher.size) {
            val enc = cipher.copyOfRange(off, off + 8)
            val eb = ByteArray(8) { i -> (enc[i].toInt() xor prevIn[i].toInt()).toByte() }
            var (v0, v1) = readWords(eb)
            val d = decryptBlock(v0, v1, k)
            val x = ByteArray(8)
            writeWord(x, 0, d.first)
            writeWord(x, 4, d.second)
            for (i in 0 until 8) out[off + i] = (x[i].toInt() xor prevOut[i].toInt()).toByte()
            prevIn = x
            prevOut = enc
            off += 8
        }
        return out
    }

    private fun encryptBlock(v0In: Int, v1In: Int, k: IntArray): Pair<Int, Int> {
        var v0 = v0In
        var v1 = v1In
        var sum = 0
        repeat(ROUNDS) {
            sum += DELTA
            v0 += ((v1 shl 4) + k[0]) xor (v1 + sum) xor ((v1 ushr 5) + k[1])
            v1 += ((v0 shl 4) + k[2]) xor (v0 + sum) xor ((v0 ushr 5) + k[3])
        }
        return v0 to v1
    }

    private fun decryptBlock(v0In: Int, v1In: Int, k: IntArray): Pair<Int, Int> {
        var v0 = v0In
        var v1 = v1In
        var sum = DELTA * ROUNDS
        repeat(ROUNDS) {
            v1 -= ((v0 shl 4) + k[2]) xor (v0 + sum) xor ((v0 ushr 5) + k[3])
            v0 -= ((v1 shl 4) + k[0]) xor (v1 + sum) xor ((v1 ushr 5) + k[1])
            sum -= DELTA
        }
        return v0 to v1
    }

    // ------------------------------------------------------------------ 工具
    fun randomBytes(n: Int): ByteArray = ByteArray(n).also(random::nextBytes)

    private fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        return mac.doFinal(data)
    }

    private fun deflate(data: ByteArray, level: Int = Deflater.DEFAULT_COMPRESSION): ByteArray {
        val deflater = Deflater(level)
        return try {
            deflater.setInput(data)
            deflater.finish()
            val buf = ByteArray(data.size + 64)
            val out = java.io.ByteArrayOutputStream(data.size / 2 + 32)
            while (!deflater.finished()) {
                val n = deflater.deflate(buf)
                out.write(buf, 0, n)
            }
            out.toByteArray()
        } finally {
            deflater.end()
        }
    }

    private fun intKey(key16: ByteArray): IntArray {
        val k = ByteArray(16)
        key16.copyInto(k, 0, 0, minOf(16, key16.size))
        return IntArray(4) { i ->
            ((k[i * 4].toInt() and 0xFF) shl 24) or ((k[i * 4 + 1].toInt() and 0xFF) shl 16) or
                ((k[i * 4 + 2].toInt() and 0xFF) shl 8) or (k[i * 4 + 3].toInt() and 0xFF)
        }
    }

    private fun readWords(b: ByteArray): Pair<Int, Int> = Pair(
        ((b[0].toInt() and 0xFF) shl 24) or ((b[1].toInt() and 0xFF) shl 16) or
            ((b[2].toInt() and 0xFF) shl 8) or (b[3].toInt() and 0xFF),
        ((b[4].toInt() and 0xFF) shl 24) or ((b[5].toInt() and 0xFF) shl 16) or
            ((b[6].toInt() and 0xFF) shl 8) or (b[7].toInt() and 0xFF),
    )

    private fun writeWord(b: ByteArray, off: Int, v: Int) {
        b[off] = (v ushr 24).toByte()
        b[off + 1] = (v ushr 16).toByte()
        b[off + 2] = (v ushr 8).toByte()
        b[off + 3] = v.toByte()
    }
}

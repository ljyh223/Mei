package com.ljyh.mei.data.network.netease

import com.github.luben.zstd.Zstd
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

/** NCBL v3 envelope, independently implemented from the official bilog layout. */
internal object NcblCodec {
    private val modulus = BigInteger("fd90bd466ff9bc8a3fec2fbcf263b90d5c564879fa5d7aab89b31c1d5cb4139d", 16)
    private val exponent = BigInteger.valueOf(65537)
    private const val FIXED_HEADER = 70
    private const val FRAME_SIZE = 32768

    fun entropy(): ByteArray = ByteArray(50).also(SecureRandom()::nextBytes)

    /** Persist entropy with the record: retries reconstruct the same UUID, ciphertext and filename. */
    fun encode(meta: ByteArray, records: ByteArray, entropy: ByteArray): ByteArray {
        require(entropy.size == 50)
        require(meta.size <= 65535 - FIXED_HEADER - 4)
        require(records.size <= 512 * 1024)
        val key = entropy.copyOfRange(0, 32)
        if ((key[0].toInt() and 255) >= 0xa3) key[0] = 0xa2.toByte()
        val wrapped = BigInteger(1, key).modPow(exponent, modulus).toByteArray().let { bytes ->
            bytes.takeLast(32).toByteArray().let { ByteArray(32 - it.size) + it }
        }
        val uuid = entropy.copyOfRange(32, 48).apply {
            this[6] = ((this[6].toInt() and 15) or 64).toByte()
            this[8] = ((this[8].toInt() and 63) or 128).toByte()
        }
        val nonce = uuid.copyOfRange(0, 12)
        val counter = littleEndian(uuid).getInt(12) ushr 2
        val encryptedMeta = chacha20(wrapped, nonce, counter, meta)
        val compressed = Zstd.compress(records)
        val frameCount = maxOf(1, (compressed.size + FRAME_SIZE - 1) / FRAME_SIZE)
        val sequence = (entropy[48].toInt() and 255) or ((entropy[49].toInt() and 255) shl 8)
        val headerSize = FIXED_HEADER + 4 + meta.size
        return ByteBuffer.allocate(headerSize + compressed.size + frameCount * 6)
            .order(ByteOrder.LITTLE_ENDIAN).apply {
                put(byteArrayOf(78, 67, 66, 76))
                putInt(3)
                putShort(headerSize.toShort())
                put(uuid)
                put(wrapped)
                putInt(sequence)
                putInt(sequence + frameCount - 1)
                putInt(compressed.size + frameCount * 6)
                putShort(0x4343.toShort())
                putShort(meta.size.toShort())
                put(encryptedMeta)
                repeat(frameCount) { frame ->
                    val offset = frame * FRAME_SIZE
                    val bytes = compressed.copyOfRange(offset, minOf(offset + FRAME_SIZE, compressed.size))
                    putShort(bytes.size.toShort())
                    putInt(sequence + frame)
                    put(chacha20(key, nonce, counter, bytes))
                }
            }.array()
    }

    internal fun chacha20(key: ByteArray, nonce: ByteArray, counter: Int, input: ByteArray): ByteArray {
        require(key.size == 32 && nonce.size == 12)
        val initial = intArrayOf(0x61707865, 0x3320646e, 0x79622d32, 0x6b206574) +
            IntArray(8) { littleEndian(key).getInt(it * 4) } +
            intArrayOf(counter) + IntArray(3) { littleEndian(nonce).getInt(it * 4) }
        val output = ByteArray(input.size)
        var offset = 0
        while (offset < input.size) {
            initial[12] = counter + offset / 64
            val state = initial.copyOf()
            repeat(10) {
                quarter(state, 0, 4, 8, 12); quarter(state, 1, 5, 9, 13)
                quarter(state, 2, 6, 10, 14); quarter(state, 3, 7, 11, 15)
                quarter(state, 0, 5, 10, 15); quarter(state, 1, 6, 11, 12)
                quarter(state, 2, 7, 8, 13); quarter(state, 3, 4, 9, 14)
            }
            val block = ByteBuffer.allocate(64).order(ByteOrder.LITTLE_ENDIAN)
            repeat(16) { block.putInt(state[it] + initial[it]) }
            repeat(minOf(64, input.size - offset)) { i ->
                output[offset + i] = (input[offset + i].toInt() xor block.array()[i].toInt()).toByte()
            }
            offset += 64
        }
        return output
    }

    private fun quarter(s: IntArray, a: Int, b: Int, c: Int, d: Int) {
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] xor s[a], 16)
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] xor s[c], 12)
        s[a] += s[b]; s[d] = Integer.rotateLeft(s[d] xor s[a], 8)
        s[c] += s[d]; s[b] = Integer.rotateLeft(s[b] xor s[c], 7)
    }

    private fun littleEndian(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
}

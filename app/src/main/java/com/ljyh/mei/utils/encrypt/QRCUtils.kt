package com.ljyh.mei.utils.encrypt

import android.util.Log
import java.io.ByteArrayInputStream
import java.util.zip.InflaterInputStream

object QRCUtils {
    enum class DESMode {
        DES_ENCRYPT,
        DES_DECRYPT
    }
    private fun bitNum(a: ByteArray, b: Int, c: Int): Long {
        val byteIndex = (b / 32) * 4 + 3 - (b % 32) / 8
        val bitPosition = 7 - (b % 8)
        val extractedBit = (a[byteIndex].toUInt() shr bitPosition) and 0x01u // 使用 UInt
        return (extractedBit shl c).toLong() // 转换为 UInt
    }

    private fun bitNumIntR(a: Long, b: Int, c: Int): Long {
        val extractedBit = (a shr (31 - b)) and 0x00000001
        return extractedBit shl c
    }

    private fun bitNumIntL(a: Long, b: Int, c: Int): Long {
        val extractedBit = (a shl b) and 0x80000000
        return extractedBit shr c
    }

    private fun sBoxBit(a: Long): Long {
        val part1 = a and 0x20
        val part2 = (a and 0x1f) shr 1
        val part3 = (a and 0x01) shl 4
        return part1 or part2 or part3
    }


    private val sBox1 = intArrayOf(
        14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7,
        0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8,
        4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0,
        15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13
    )


    private val sBox2 = intArrayOf(
        15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10,
        3, 13, 4, 7, 15, 2, 8, 15, 12, 0, 1, 10, 6, 9, 11, 5,
        0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15,
        13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9
    )

    private val sBox3 = intArrayOf(
        10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8,
        13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1,
        13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7,
        1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12
    )


    private val sBox4 = intArrayOf(
        7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15,
        13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9,
        10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4,
        3, 15, 0, 6, 10, 10, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14
    )


    private val sBox5 = intArrayOf(
        2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9,
        14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6,
        4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14,
        11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3
    )


    private val sBox6 = intArrayOf(
        12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11,
        10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8,
        9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6,
        4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13
    )


    private val sBox7 = intArrayOf(
        4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1,
        13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6,
        1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2,
        6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12
    )


    private val sBox8 = intArrayOf(
        13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7,
        1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2,
        7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8,
        2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11
    )


    private fun ip(state: LongArray, inBytes: ByteArray): LongArray {
        state[0] = (
                bitNum(inBytes, 57, 31) or bitNum(inBytes, 49, 30) or bitNum(inBytes, 41, 29) or
                        bitNum(inBytes, 33, 28) or bitNum(inBytes, 25, 27) or bitNum(
                    inBytes,
                    17,
                    26
                ) or
                        bitNum(inBytes, 9, 25) or bitNum(inBytes, 1, 24) or bitNum(
                    inBytes,
                    59,
                    23
                ) or
                        bitNum(inBytes, 51, 22) or bitNum(inBytes, 43, 21) or bitNum(
                    inBytes,
                    35,
                    20
                ) or
                        bitNum(inBytes, 27, 19) or bitNum(inBytes, 19, 18) or bitNum(
                    inBytes,
                    11,
                    17
                ) or
                        bitNum(inBytes, 3, 16) or bitNum(inBytes, 61, 15) or bitNum(
                    inBytes,
                    53,
                    14
                ) or
                        bitNum(inBytes, 45, 13) or bitNum(inBytes, 37, 12) or bitNum(
                    inBytes,
                    29,
                    11
                ) or
                        bitNum(inBytes, 21, 10) or bitNum(inBytes, 13, 9) or bitNum(
                    inBytes,
                    5,
                    8
                ) or
                        bitNum(inBytes, 63, 7) or bitNum(inBytes, 55, 6) or bitNum(
                    inBytes,
                    47,
                    5
                ) or
                        bitNum(inBytes, 39, 4) or bitNum(inBytes, 31, 3) or bitNum(
                    inBytes,
                    23,
                    2
                ) or
                        bitNum(inBytes, 15, 1) or bitNum(inBytes, 7, 0)
                )

        state[1] = (
                bitNum(inBytes, 56, 31) or bitNum(inBytes, 48, 30) or bitNum(inBytes, 40, 29) or
                        bitNum(inBytes, 32, 28) or bitNum(inBytes, 24, 27) or bitNum(
                    inBytes,
                    16,
                    26
                ) or
                        bitNum(inBytes, 8, 25) or bitNum(inBytes, 0, 24) or bitNum(
                    inBytes,
                    58,
                    23
                ) or
                        bitNum(inBytes, 50, 22) or bitNum(inBytes, 42, 21) or bitNum(
                    inBytes,
                    34,
                    20
                ) or
                        bitNum(inBytes, 26, 19) or bitNum(inBytes, 18, 18) or bitNum(
                    inBytes,
                    10,
                    17
                ) or
                        bitNum(inBytes, 2, 16) or bitNum(inBytes, 60, 15) or bitNum(
                    inBytes,
                    52,
                    14
                ) or
                        bitNum(inBytes, 44, 13) or bitNum(inBytes, 36, 12) or bitNum(
                    inBytes,
                    28,
                    11
                ) or
                        bitNum(inBytes, 20, 10) or bitNum(inBytes, 12, 9) or bitNum(
                    inBytes,
                    4,
                    8
                ) or
                        bitNum(inBytes, 62, 7) or bitNum(inBytes, 54, 6) or bitNum(
                    inBytes,
                    46,
                    5
                ) or
                        bitNum(inBytes, 38, 4) or bitNum(inBytes, 30, 3) or bitNum(
                    inBytes,
                    22,
                    2
                ) or
                        bitNum(inBytes, 14, 1) or bitNum(inBytes, 6, 0)
                )
        return state
    }

    private fun invIp(state: LongArray, outBytes: ByteArray): ByteArray {
        outBytes[3] = (
                bitNumIntR(state[1], 7, 7) or bitNumIntR(state[0], 7, 6) or
                        bitNumIntR(state[1], 15, 5) or bitNumIntR(state[0], 15, 4) or
                        bitNumIntR(state[1], 23, 3) or bitNumIntR(state[0], 23, 2) or
                        bitNumIntR(state[1], 31, 1) or bitNumIntR(state[0], 31, 0)
                ).toByte()
        outBytes[2] = (
                bitNumIntR(state[1], 6, 7) or bitNumIntR(state[0], 6, 6) or
                        bitNumIntR(state[1], 14, 5) or bitNumIntR(state[0], 14, 4) or
                        bitNumIntR(state[1], 22, 3) or bitNumIntR(state[0], 22, 2) or
                        bitNumIntR(state[1], 30, 1) or bitNumIntR(state[0], 30, 0)
                ).toByte()
        outBytes[1] = (
                bitNumIntR(state[1], 5, 7) or bitNumIntR(state[0], 5, 6) or
                        bitNumIntR(state[1], 13, 5) or bitNumIntR(state[0], 13, 4) or
                        bitNumIntR(state[1], 21, 3) or bitNumIntR(state[0], 21, 2) or
                        bitNumIntR(state[1], 29, 1) or bitNumIntR(state[0], 29, 0)
                ).toByte()
        outBytes[0] = (
                bitNumIntR(state[1], 4, 7) or bitNumIntR(state[0], 4, 6) or
                        bitNumIntR(state[1], 12, 5) or bitNumIntR(state[0], 12, 4) or
                        bitNumIntR(state[1], 20, 3) or bitNumIntR(state[0], 20, 2) or
                        bitNumIntR(state[1], 28, 1) or bitNumIntR(state[0], 28, 0)
                ).toByte()
        outBytes[7] = (
                bitNumIntR(state[1], 3, 7) or bitNumIntR(state[0], 3, 6) or
                        bitNumIntR(state[1], 11, 5) or bitNumIntR(state[0], 11, 4) or
                        bitNumIntR(state[1], 19, 3) or bitNumIntR(state[0], 19, 2) or
                        bitNumIntR(state[1], 27, 1) or bitNumIntR(state[0], 27, 0)
                ).toByte()
        outBytes[6] = (
                bitNumIntR(state[1], 2, 7) or bitNumIntR(state[0], 2, 6) or
                        bitNumIntR(state[1], 10, 5) or bitNumIntR(state[0], 10, 4) or
                        bitNumIntR(state[1], 18, 3) or bitNumIntR(state[0], 18, 2) or
                        bitNumIntR(state[1], 26, 1) or bitNumIntR(state[0], 26, 0)
                ).toByte()
        outBytes[5] = (
                bitNumIntR(state[1], 1, 7) or bitNumIntR(state[0], 1, 6) or
                        bitNumIntR(state[1], 9, 5) or bitNumIntR(state[0], 9, 4) or
                        bitNumIntR(state[1], 17, 3) or bitNumIntR(state[0], 17, 2) or
                        bitNumIntR(state[1], 25, 1) or bitNumIntR(state[0], 25, 0)
                ).toByte()
        outBytes[4] = (
                bitNumIntR(state[1], 0, 7) or bitNumIntR(state[0], 0, 6) or
                        bitNumIntR(state[1], 8, 5) or bitNumIntR(state[0], 8, 4) or
                        bitNumIntR(state[1], 16, 3) or bitNumIntR(state[0], 16, 2) or
                        bitNumIntR(state[1], 24, 1) or bitNumIntR(state[0], 24, 0)
                ).toByte()
        return outBytes
    }


    private fun f(state: Long, key: LongArray): Long {
        val lrgstate = LongArray(6)

        // Expansion Permutation
        val t1 = (bitNumIntL(state, 31, 0) or ((state and 0xf0000000) shr 1) or bitNumIntL(
            state,
            4,
            5
        ) or
                bitNumIntL(state, 3, 6) or ((state and 0x0f000000) shr 3) or bitNumIntL(
            state,
            8,
            11
        ) or
                bitNumIntL(state, 7, 12) or ((state and 0x00f00000) shr 5) or bitNumIntL(
            state,
            12,
            17
        ) or
                bitNumIntL(state, 11, 18) or ((state and 0x000f0000) shr 7) or bitNumIntL(
            state,
            16,
            23
        ))

        val t2 =
            (bitNumIntL(state, 15, 0) or ((state and 0x0000f000) shl 15) or bitNumIntL(
                state,
                20,
                5
            ) or
                    bitNumIntL(state, 19, 6) or ((state and 0x00000f00) shl 13) or bitNumIntL(
                state,
                24,
                11
            ) or
                    bitNumIntL(state, 23, 12) or ((state and 0x000000f0) shl 11) or bitNumIntL(
                state,
                28,
                17
            ) or
                    bitNumIntL(state, 27, 18) or ((state and 0x0000000f) shl 9) or bitNumIntL(
                state,
                0,
                23
            ))

        lrgstate[0] = (t1 shr 24) and 0x000000ff
        lrgstate[1] = (t1 shr 16) and 0x000000ff
        lrgstate[2] = (t1 shr 8) and 0x000000ff
        lrgstate[3] = (t2 shr 24) and 0x000000ff
        lrgstate[4] = (t2 shr 16) and 0x000000ff
        lrgstate[5] = (t2 shr 8) and 0x000000ff

        // Key XOR
        for (i in 0..5) {
            lrgstate[i] = lrgstate[i] xor key[i]
        }

        // S-Box Permutation
        var newState = ((sBox1[sBoxBit(lrgstate[0] shr 2).toInt()] shl 28) or  // Changed to var
                (sBox2[sBoxBit((lrgstate[0] and 0x03) shl 4 or (lrgstate[1] shr 4)).toInt()] shl 24) or
                (sBox3[sBoxBit((lrgstate[1] and 0x0f) shl 2 or (lrgstate[2] shr 6)).toInt()] shl 20) or
                (sBox4[sBoxBit(lrgstate[2] and 0x3f).toInt()] shl 16) or
                (sBox5[sBoxBit(lrgstate[3] shr 2).toInt()] shl 12) or
                (sBox6[sBoxBit((lrgstate[3] and 0x03) shl 4 or (lrgstate[4] shr 4)).toInt()] shl 8) or
                (sBox7[sBoxBit((lrgstate[4] and 0x0f) shl 2 or (lrgstate[5] shr 6)).toInt()] shl 4) or
                sBox8[sBoxBit(lrgstate[5] and 0x3f).toInt()]).toLong()

        // P-Box Permutation
        newState = (bitNumIntL(newState, 15, 0) or bitNumIntL(newState, 6, 1) or bitNumIntL(
            newState,
            19,
            2
        ) or // Changed to newState
                bitNumIntL(newState, 20, 3) or bitNumIntL(newState, 28, 4) or bitNumIntL(
            newState,
            11,
            5
        ) or
                bitNumIntL(newState, 27, 6) or bitNumIntL(newState, 16, 7) or bitNumIntL(
            newState,
            0,
            8
        ) or
                bitNumIntL(newState, 14, 9) or bitNumIntL(newState, 22, 10) or bitNumIntL(
            newState,
            25,
            11
        ) or
                bitNumIntL(newState, 4, 12) or bitNumIntL(newState, 17, 13) or bitNumIntL(
            newState,
            30,
            14
        ) or
                bitNumIntL(newState, 9, 15) or bitNumIntL(newState, 1, 16) or bitNumIntL(
            newState,
            7,
            17
        ) or
                bitNumIntL(newState, 23, 18) or bitNumIntL(newState, 13, 19) or bitNumIntL(
            newState,
            31,
            20
        ) or
                bitNumIntL(newState, 26, 21) or bitNumIntL(newState, 2, 22) or bitNumIntL(
            newState,
            8,
            23
        ) or
                bitNumIntL(newState, 18, 24) or bitNumIntL(newState, 12, 25) or bitNumIntL(
            newState,
            29,
            26
        ) or
                bitNumIntL(newState, 5, 27) or bitNumIntL(newState, 21, 28) or bitNumIntL(
            newState,
            10,
            29
        ) or
                bitNumIntL(newState, 3, 30) or bitNumIntL(newState, 24, 31))

        return newState // Return the modified state
    }

    private fun desKeySetup(key: ByteArray, schedule: Array<LongArray>, mode: DESMode): Int {
        val keyRndShift = intArrayOf(1, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 2, 1)
        val keyPermC = intArrayOf(
            56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17,
            9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35
        )
        val keyPermD = intArrayOf(
            62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21,
            13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3
        )
        val keyCompression = intArrayOf(
            13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9,
            22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1,
            40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47,
            43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31
        )

        // Permutated Choice #1 (copy the key in, ignoring parity bits).
        var c = 0L
        var d = 0L
        for (i in 0..27) {
            c = c or bitNum(key, keyPermC[i], 31 - i)
            d = d or bitNum(key, keyPermD[i], 31 - i)
        }

        // Generate the 16 subkeys.
        for (i in 0..15) {
            c = ((c shl keyRndShift[i]) or (c shr (28 - keyRndShift[i]))) and 0xfffffff0
            d = ((d shl keyRndShift[i]) or (d shr (28 - keyRndShift[i]))) and 0xfffffff0

            // Decryption subkeys are reverse order of encryption subkeys
            val toGen = if (mode == DESMode.DES_DECRYPT) 15 - i else i

            // Initialize the array
            schedule[toGen] = LongArray(6)
            for (j in 0..23) {
                schedule[toGen][j / 8] =
                    schedule[toGen][j / 8] or bitNumIntR(c, keyCompression[j], 7 - (j % 8))
            }
            for (j in 24..47) {
                schedule[toGen][j / 8] =
                    schedule[toGen][j / 8] or bitNumIntR(d, keyCompression[j] - 27, 7 - (j % 8))
            }
        }
        return 0
    }

    private fun desCrypt(inputBytes: ByteArray, keySchedule: Array<LongArray>): ByteArray {
        val state = LongArray(2)

        // Initial Permutation
        ip(state, inputBytes)
        for (idx in 0..14) {
            val t = state[1]
            val i = f(state[1], keySchedule[idx])
            state[1] = i xor state[0]
            state[0] = t
        }

        // Perform the final loop manually as it doesn't switch sides
        state[0] = f(state[1], keySchedule[15]) xor state[0]

        // Inverse Initial Permutation
        return invIp(state, inputBytes)
    }


    private val KEY1 = "!@#)(NHLiuy*$%^&".toByteArray()
    private val KEY2 = "123ZXC!@#)(*$%^&".toByteArray()
    private val KEY3 = "!@#)(*$%^&abcDEF".toByteArray()

    private fun funcDes(buff: ByteArray, key: ByteArray, length: Int): ByteArray {
        val schedule = Array(16) { LongArray(6) }
        desKeySetup(key, schedule, DESMode.DES_ENCRYPT)
        var output = ByteArray(0) // Initialize as empty array
        for (i in 0 until length step 8) {
            val block = buff.copyOfRange(i, minOf(i + 8, length)) // Handle partial blocks
            val paddedBlock = if (block.size < 8) {
                block.copyOf(8)  // Pad with zeros if necessary
            } else {
                block
            }
            output += desCrypt(paddedBlock, schedule)
        }
        return output
    }

    private fun funcDdes(buff: ByteArray, key: ByteArray, length: Int): ByteArray {
        val schedule = Array(16) { LongArray(6) }
        desKeySetup(key, schedule, DESMode.DES_DECRYPT)
        var output = ByteArray(0) // Initialize as empty array
        for (i in 0 until length step 8) {
            val block = buff.copyOfRange(i, minOf(i + 8, length)) // Handle partial blocks
            val paddedBlock = if (block.size < 8) {
                block.copyOf(8) // Pad with zeros if necessary
            } else {
                block
            }
            output += desCrypt(paddedBlock, schedule)
        }
        return output
    }

    private fun lyricDecode(data: ByteArray, length: Int): ByteArray {
        var content = funcDdes(data, KEY1, length)
        content = funcDes(content, KEY2, length)
        content = funcDdes(content, KEY3, length)
        return content
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        return ByteArray(s.length / 2) { i ->
            Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16).toByte()
        }
    }

    private fun decompressZlib(data: ByteArray): ByteArray {
        return InflaterInputStream(ByteArrayInputStream(data)).readBytes()
    }

    fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02X".format(it) }
    }

    //传入歌词
    fun decodeLyric(data: String,isTranslation:Boolean=false): String {
        if(data=="") return ""
        try {
            val decodedData = hexStringToByteArray(data)
            val decodedContents = lyricDecode(decodedData, decodedData.size)
            val gzipInputStream = decompressZlib(decodedContents)
            val lyric = gzipInputStream.toString(Charsets.UTF_8)
            if(isTranslation) return lyric

            val extractXmlRe =Regex("""<Lyric_1 LyricType="1" LyricContent="(.*?)"/>""", RegexOption.DOT_MATCHES_ALL)

            val qrcLyric = extractXmlRe.find(lyric)
            if (qrcLyric != null) {
                return qrcLyric.groupValues[1]
            } else {
                return ""

            }
        }catch (e: Exception){
            Log.e("QRCUtils", "decodeLyric: ${e.message}")
            return ""
        }
    }
}
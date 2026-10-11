package com.ljyh.mei.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class UsbExclusiveOutputProviderTest {
    @Test
    fun signed24BitPcmRoundTripsThroughFloatToInt32() {
        for (value in intArrayOf(-8_388_608, -4_194_305, -1, 0, 1, 4_194_305, 8_388_607)) {
            val decodedFloat = value.toFloat() / 8_388_608f
            assertEquals(value shl 8, floatPcmToInt32(decodedFloat))
        }
    }

    @Test
    fun signed16BitPcmRoundTripsThroughFloatToInt32() {
        for (value in intArrayOf(-32_768, -12_345, -1, 0, 1, 12_345, 32_767)) {
            val decodedFloat = value.toFloat() / 32_768f
            assertEquals(value shl 16, floatPcmToInt32(decodedFloat))
        }
    }

    @Test
    fun invalidAndClippingFloatValuesAreSafe() {
        assertEquals(0, floatPcmToInt32(Float.NaN))
        assertEquals(0, floatPcmToInt32(Float.POSITIVE_INFINITY))
        assertEquals(Int.MIN_VALUE, floatPcmToInt32(-1.5f))
        assertEquals(Int.MAX_VALUE, floatPcmToInt32(1.5f))
    }
}

package com.ljyh.mei.playback

import androidx.media3.common.audio.ChannelMixingMatrix

/** Channel order follows Android's PCM layouts: L, R, C, LFE, rear L, rear R. */
internal fun stereoDownmixMatrix(channels: Int): ChannelMixingMatrix {
    val coefficients = when (channels) {
        1 -> floatArrayOf(0.7071f, 0.7071f)
        2 -> floatArrayOf(1f, 0f, 0f, 1f)
        3 -> floatArrayOf(1f, 0f, 0f, 1f, 0.7071f, 0.7071f)
        4 -> floatArrayOf(1f, 0f, 0f, 1f, 0.7071f, 0f, 0f, 0.7071f)
        5 -> floatArrayOf(1f, 0f, 0f, 1f, 0.7071f, 0.7071f, 0.7071f, 0f, 0f, 0.7071f)
        6 -> floatArrayOf(
            1f, 0f, 0f, 1f, 0.7071f, 0.7071f,
            0.5f, 0.5f, 0.7071f, 0f, 0f, 0.7071f,
        )
        7, 8 -> FloatArray(channels * channels) { index ->
            if (index / channels == index % channels) 1f else 0f
        }
        else -> error("Unsupported channel count: $channels")
    }
    return ChannelMixingMatrix(channels, if (channels > 6) channels else 2, coefficients)
}

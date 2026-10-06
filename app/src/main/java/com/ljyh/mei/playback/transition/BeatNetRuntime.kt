package com.ljyh.mei.playback.transition

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Loads the original BeatNet model's float tensors; inference runs in Mei's native code. */
internal class BeatNetRuntime(context: Context) {
    private val model: ByteBuffer = context.assets.open("beatnet/model_1_f32.bin").use { input ->
        val bytes = input.readBytes()
        require(bytes.size == MODEL_BYTES) { "Unexpected BeatNet model size" }
        ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put(bytes)
            flip()
        }
    }

    fun run(features: FloatArray): FloatArray {
        require(features.size == FRAME_COUNT * FEATURE_COUNT)
        return requireNotNull(predict(model, features)) { "BeatNet native inference failed" }
    }

    private external fun predict(weights: ByteBuffer, features: FloatArray): FloatArray?

    private companion object {
        const val FRAME_COUNT = 1_600
        const val FEATURE_COUNT = 272
        const val MODEL_BYTES = 1_609_300
        init { System.loadLibrary("mei_beatnet") }
    }
}

package com.ljyh.mei.audio.match

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

internal const val MatchDurationSeconds = 3
internal const val MatchSampleRate = 8_000
internal const val MatchSampleCount = MatchDurationSeconds * MatchSampleRate

class AudioSampleRecorder @Inject constructor() {
    suspend fun record(): FloatArray = withContext(Dispatchers.IO) {
        val minimumBytes = AudioRecord.getMinBufferSize(
            MatchSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minimumBytes <= 0) throw IOException("设备不支持 8 kHz 麦克风录音")

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MatchSampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minimumBytes, MatchSampleRate * Short.SIZE_BYTES),
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IOException("麦克风初始化失败")
        }

        val effects = listOfNotNull(
            runCatching { AcousticEchoCanceler.create(recorder.audioSessionId) }.getOrNull(),
            runCatching { AutomaticGainControl.create(recorder.audioSessionId) }.getOrNull(),
            runCatching { NoiseSuppressor.create(recorder.audioSessionId) }.getOrNull(),
        )
        try {
            effects.forEach { runCatching { it.enabled = false } }
            val samples = FloatArray(MatchSampleCount)
            val buffer = ShortArray(1_024)
            var written = 0
            recorder.startRecording()
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw IOException("麦克风未开始录音")
            }
            while (written < samples.size) {
                currentCoroutineContext().ensureActive()
                val read = recorder.read(
                    buffer,
                    0,
                    minOf(buffer.size, samples.size - written),
                    AudioRecord.READ_BLOCKING,
                )
                if (read <= 0) throw IOException("录音中断（错误码 $read）")
                repeat(read) { index -> samples[written + index] = buffer[index] / 32768f }
                written += read
            }
            samples
        } finally {
            if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                runCatching { recorder.stop() }
            }
            effects.forEach(AudioEffect::release)
            recorder.release()
        }
    }
}

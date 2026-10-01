package com.ljyh.mei.playback

import android.content.Context
import com.ljyh.mei.constants.EqualizerBandLevelsKey
import com.ljyh.mei.constants.EqualizerEnabledKey
import com.ljyh.mei.constants.ParametricEqualizerProfileKey
import com.ljyh.mei.playback.equalizer.EqFilter
import com.ljyh.mei.playback.equalizer.EqualizerProfile
import com.ljyh.mei.playback.equalizer.EqualizerProfileCodec
import com.ljyh.mei.playback.equalizer.FilterType
import com.ljyh.mei.playback.equalizer.ParametricEqualizerProcessor
import com.ljyh.mei.utils.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Keeps playback DSP configuration in sync with the editor and migrates the earlier 5-band EQ. */
class AudioEffectsController(
    context: Context,
    scope: CoroutineScope,
    private val processor: ParametricEqualizerProcessor,
) {
    private val preferenceJob: Job = scope.launch {
        context.dataStore.data.collectLatest { preferences ->
            val profile = EqualizerProfileCodec.decode(preferences[ParametricEqualizerProfileKey])
                ?: legacyProfile(preferences[EqualizerEnabledKey] ?: false, preferences[EqualizerBandLevelsKey])
            processor.setProfile(profile)
        }
    }

    fun release() = preferenceJob.cancel()

    companion object {
        fun legacyProfile(enabled: Boolean, encodedLevels: String?): EqualizerProfile {
            val levels = encodedLevels?.split(',')?.mapNotNull(String::toIntOrNull).orEmpty()
            val frequencies = listOf(60f, 230f, 910f, 3_600f, 14_000f)
            return EqualizerProfile(
                enabled = enabled,
                presetId = "custom",
                filters = frequencies.mapIndexed { index, frequency ->
                    EqFilter(index + 1, FilterType.PEAK, frequency,
                        (levels.getOrElse(index) { 0 } / 100f).coerceIn(-12f, 12f))
                },
            )
        }
    }
}

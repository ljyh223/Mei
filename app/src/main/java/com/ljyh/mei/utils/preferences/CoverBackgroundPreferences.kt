package com.ljyh.mei.utils.preferences

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.CoverBackgroundPresetKey
import com.ljyh.mei.constants.CoverBackgroundToleranceKey
import com.ljyh.mei.constants.CoverBackgroundConfidenceKey
import com.ljyh.mei.constants.CoverSolidEdgeMatchKey
import com.ljyh.mei.constants.CoverSoftEdgeMatchKey
import com.ljyh.mei.constants.CoverSingleEdgeMatchKey
import com.ljyh.mei.constants.CoverMaximumTextureKey
import com.ljyh.mei.constants.CoverMaximumGradientKey
import com.ljyh.mei.constants.CoverMinimumBandShareKey
import com.ljyh.mei.constants.CoverMinimumWholeShareKey
import com.ljyh.mei.constants.CoverMaximumSolidGapKey
import com.ljyh.mei.constants.CoverMaximumSoftGapKey
import com.ljyh.mei.constants.CoverSolidSideSlackKey
import com.ljyh.mei.constants.CoverSingleEdgeStrongMatchKey
import com.ljyh.mei.constants.CoverThreeSideMinimumMatchKey
import com.ljyh.mei.constants.CoverAllowSingleOccupiedSideKey
import com.ljyh.mei.constants.CoverAllowThreeStrongSidesKey
import com.ljyh.mei.utils.image.CoverBackgroundConfig
import com.ljyh.mei.utils.image.CoverBackgroundPreset
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class CoverBackgroundSettingsState(
    val config: CoverBackgroundConfig = CoverBackgroundConfig(),
    val preset: CoverBackgroundPreset = CoverBackgroundPreset.Standard,
)

internal fun Preferences.coverBackgroundSettings(): CoverBackgroundSettingsState {
    val defaults = CoverBackgroundConfig()
    val config = CoverBackgroundConfig(
        colorTolerance = this[CoverBackgroundToleranceKey] ?: defaults.colorTolerance,
        minimumBackgroundConfidence = this[CoverBackgroundConfidenceKey] ?: defaults.minimumBackgroundConfidence,
        solidEdgeMatch = this[CoverSolidEdgeMatchKey] ?: defaults.solidEdgeMatch,
        softEdgeMatch = this[CoverSoftEdgeMatchKey] ?: defaults.softEdgeMatch,
        minimumSingleEdgeMatch = this[CoverSingleEdgeMatchKey] ?: defaults.minimumSingleEdgeMatch,
        maximumBandTexture = this[CoverMaximumTextureKey] ?: defaults.maximumBandTexture,
        maximumBandGradient = this[CoverMaximumGradientKey] ?: defaults.maximumBandGradient,
        minimumBandShare = this[CoverMinimumBandShareKey] ?: defaults.minimumBandShare,
        minimumWholeShare = this[CoverMinimumWholeShareKey] ?: defaults.minimumWholeShare,
        maximumSolidGap = this[CoverMaximumSolidGapKey] ?: defaults.maximumSolidGap,
        maximumSoftGap = this[CoverMaximumSoftGapKey] ?: defaults.maximumSoftGap,
        solidSideSlack = this[CoverSolidSideSlackKey] ?: defaults.solidSideSlack,
        singleEdgeStrongMatch = this[CoverSingleEdgeStrongMatchKey] ?: defaults.singleEdgeStrongMatch,
        threeSideMinimumMatch = this[CoverThreeSideMinimumMatchKey] ?: defaults.threeSideMinimumMatch,
        allowSingleOccupiedSide = this[CoverAllowSingleOccupiedSideKey] ?: defaults.allowSingleOccupiedSide,
        allowThreeStrongSides = this[CoverAllowThreeStrongSidesKey] ?: defaults.allowThreeStrongSides,
    ).normalized()
    val preset = if (this[CoverBackgroundPresetKey] == CoverBackgroundPreset.Custom.name) {
        CoverBackgroundPreset.Custom
    } else {
        CoverBackgroundPreset.entries.firstOrNull { it.config == config } ?: CoverBackgroundPreset.Custom
    }
    return CoverBackgroundSettingsState(config, preset)
}

internal fun MutablePreferences.setCoverBackgroundSettings(
    config: CoverBackgroundConfig,
    preset: CoverBackgroundPreset,
) {
    val value = config.normalized()
    this[CoverBackgroundPresetKey] = preset.name
    this[CoverBackgroundToleranceKey] = value.colorTolerance
    this[CoverBackgroundConfidenceKey] = value.minimumBackgroundConfidence
    this[CoverSolidEdgeMatchKey] = value.solidEdgeMatch
    this[CoverSoftEdgeMatchKey] = value.softEdgeMatch
    this[CoverSingleEdgeMatchKey] = value.minimumSingleEdgeMatch
    this[CoverMaximumTextureKey] = value.maximumBandTexture
    this[CoverMaximumGradientKey] = value.maximumBandGradient
    this[CoverMinimumBandShareKey] = value.minimumBandShare
    this[CoverMinimumWholeShareKey] = value.minimumWholeShare
    this[CoverMaximumSolidGapKey] = value.maximumSolidGap
    this[CoverMaximumSoftGapKey] = value.maximumSoftGap
    this[CoverSolidSideSlackKey] = value.solidSideSlack
    this[CoverSingleEdgeStrongMatchKey] = value.singleEdgeStrongMatch
    this[CoverThreeSideMinimumMatchKey] = value.threeSideMinimumMatch
    this[CoverAllowSingleOccupiedSideKey] = value.allowSingleOccupiedSide
    this[CoverAllowThreeStrongSidesKey] = value.allowThreeStrongSides
}

@Composable
internal fun rememberCoverBackgroundSettings(): State<CoverBackgroundSettingsState> {
    val context = LocalContext.current
    return remember(context) {
        context.dataStore.data.map { it.coverBackgroundSettings() }.distinctUntilChanged()
    }.collectAsState(initial = CoverBackgroundSettingsState())
}

internal suspend fun Context.saveCoverBackgroundSettings(
    config: CoverBackgroundConfig,
    preset: CoverBackgroundPreset = CoverBackgroundPreset.Custom,
) {
    dataStore.edit { it.setCoverBackgroundSettings(config, preset) }
}

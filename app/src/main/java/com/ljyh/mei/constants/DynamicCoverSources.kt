package com.ljyh.mei.constants

import androidx.datastore.preferences.core.Preferences

data class DynamicCoverSources(
    val appleMusic: Boolean,
    val netease: Boolean,
) {
    val anyEnabled: Boolean get() = appleMusic || netease

    companion object {
        val Disabled = DynamicCoverSources(appleMusic = false, netease = false)
    }
}

data class DynamicCoverSettings(
    val enabled: Boolean,
    val selectedSources: DynamicCoverSources,
) {
    val activeSources: DynamicCoverSources
        get() = if (enabled) selectedSources else DynamicCoverSources.Disabled
}

/** Preserve both the original master switch and selections saved by the earlier split-switch UI. */
fun Preferences.dynamicCoverSettings(): DynamicCoverSettings {
    val legacyEnabled = this[DynamicCoverKey] ?: false
    val appleSelection = this[AppleDynamicCoverKey]
    val neteaseSelection = this[NeteaseDynamicCoverKey]
    // A fresh master switch starts with both sources selected. Existing per-source choices
    // retain the old switch as the default for whichever source was never changed.
    val sourceDefault = if (appleSelection == null && neteaseSelection == null) true else legacyEnabled
    val selectedSources = DynamicCoverSources(
        appleMusic = appleSelection ?: sourceDefault,
        netease = neteaseSelection ?: sourceDefault,
    )
    return DynamicCoverSettings(
        enabled = this[DynamicCoverEnabledKey] ?:
            (legacyEnabled || appleSelection == true || neteaseSelection == true),
        selectedSources = selectedSources,
    )
}

fun Preferences.dynamicCoverSources(): DynamicCoverSources = dynamicCoverSettings().activeSources

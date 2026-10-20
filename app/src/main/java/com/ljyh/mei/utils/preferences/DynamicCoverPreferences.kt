package com.ljyh.mei.utils.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ljyh.mei.constants.DynamicCoverSettings
import com.ljyh.mei.constants.DynamicCoverSources
import com.ljyh.mei.constants.dynamicCoverSettings
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun rememberDynamicCoverSettings(): State<DynamicCoverSettings> {
    val context = LocalContext.current
    val settings = remember(context) {
        context.dataStore.data.map { it.dynamicCoverSettings() }.distinctUntilChanged()
    }
    return settings.collectAsState(
        initial = DynamicCoverSettings(enabled = false, selectedSources = DynamicCoverSources.Disabled)
    )
}

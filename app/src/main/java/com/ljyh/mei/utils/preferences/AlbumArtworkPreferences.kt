package com.ljyh.mei.utils.preferences

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.AlbumArtworkMode
import com.ljyh.mei.constants.AlbumArtworkModeKey
import com.ljyh.mei.constants.AlbumModelMode
import com.ljyh.mei.constants.AlbumModelModeKey
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal data class AlbumArtworkPreferences(
    val mode: AlbumArtworkMode = AlbumArtworkMode.Auto,
    val model: AlbumModelMode = AlbumModelMode.Auto,
)

internal fun Preferences.albumArtworkPreferences() = AlbumArtworkPreferences(
    this[AlbumArtworkModeKey].toEnum(AlbumArtworkMode.Auto),
    this[AlbumModelModeKey].toEnum(AlbumModelMode.Auto),
)

/** Entering the model category starts with year selection; choosing a model is a separate intent. */
internal fun MutablePreferences.setAlbumArtworkMode(mode: AlbumArtworkMode) {
    if (mode == AlbumArtworkMode.Model && albumArtworkPreferences().mode != AlbumArtworkMode.Model) {
        this[AlbumModelModeKey] = AlbumModelMode.Auto.name
    }
    this[AlbumArtworkModeKey] = mode.name
}

internal fun MutablePreferences.setAlbumModelMode(model: AlbumModelMode) {
    this[AlbumArtworkModeKey] = AlbumArtworkMode.Model.name
    this[AlbumModelModeKey] = model.name
}

internal data class AlbumArtworkPreferenceState(
    val value: AlbumArtworkPreferences,
    val onModeChange: (AlbumArtworkMode) -> Unit,
    val onModelChange: (AlbumModelMode) -> Unit,
)

/** Both entry points observe one snapshot and update both keys atomically. */
@Composable
internal fun rememberAlbumArtworkPreferences(): AlbumArtworkPreferenceState {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val flow = remember(context) {
        context.dataStore.data.map { it.albumArtworkPreferences() }.distinctUntilChanged()
    }
    val value by flow.collectAsState(initial = AlbumArtworkPreferences())
    return AlbumArtworkPreferenceState(value,
        onModeChange = { mode -> scope.launch { context.dataStore.edit { it.setAlbumArtworkMode(mode) } } },
        onModelChange = { model -> scope.launch { context.dataStore.edit { it.setAlbumModelMode(model) } } },
    )
}

package com.ljyh.mei.utils.lyric

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ljyh.mei.ui.model.LyricVariant
import com.ljyh.mei.utils.preferences.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricSelectionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private fun key(songId: String) = stringPreferencesKey("lyricVariant:$songId")

    suspend fun get(songId: String): LyricVariant =
        context.dataStore.data.first()[key(songId)]
            ?.let { stored -> LyricVariant.entries.firstOrNull { it.name == stored } }
            ?: LyricVariant.Auto

    suspend fun set(songId: String, variant: LyricVariant) {
        context.dataStore.edit { preferences ->
            val key = key(songId)
            if (variant == LyricVariant.Auto) preferences.remove(key)
            else preferences[key] = variant.name
        }
    }
}

package com.ljyh.mei.ui.screen.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.SearchHistoryKey
import com.ljyh.mei.utils.preferences.dataStore
import org.json.JSONArray

private const val MAX_SEARCH_HISTORY = 20

internal fun decodeSearchHistory(value: String?): List<String> =
    runCatching {
        val json = JSONArray(value.orEmpty())
        List(json.length()) { index -> json.optString(index) }
            .filter(String::isNotBlank)
            .distinctBy(String::lowercase)
            .take(MAX_SEARCH_HISTORY)
    }.getOrDefault(emptyList())

private fun encodeSearchHistory(history: List<String>): String =
    JSONArray(history).toString()

internal suspend fun Context.recordSearchHistory(query: String) {
    val keyword = query.trim()
    if (keyword.isBlank()) return
    dataStore.edit { preferences ->
        val history = decodeSearchHistory(preferences[SearchHistoryKey])
        preferences[SearchHistoryKey] = encodeSearchHistory(
            (listOf(keyword) + history.filterNot { it.equals(keyword, ignoreCase = true) })
                .take(MAX_SEARCH_HISTORY),
        )
    }
}

internal suspend fun Context.removeSearchHistory(query: String) {
    dataStore.edit { preferences ->
        val history = decodeSearchHistory(preferences[SearchHistoryKey])
        preferences[SearchHistoryKey] = encodeSearchHistory(
            history.filterNot { it.equals(query, ignoreCase = true) },
        )
    }
}

internal suspend fun Context.clearSearchHistory() {
    dataStore.edit { it[SearchHistoryKey] = encodeSearchHistory(emptyList()) }
}

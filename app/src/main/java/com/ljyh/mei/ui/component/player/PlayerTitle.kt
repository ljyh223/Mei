package com.ljyh.mei.ui.component.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.ljyh.mei.constants.HidePlayerTitleBracketsKey
import com.ljyh.mei.utils.preferences.rememberPreference
import com.ljyh.mei.utils.text.formatPlayerTitle

@Composable
fun rememberPlayerDisplayTitle(title: String): String {
    val hideBrackets by rememberPreference(HidePlayerTitleBracketsKey, defaultValue = false)
    return remember(title, hideBrackets) { formatPlayerTitle(title, hideBrackets) }
}

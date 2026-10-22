package com.ljyh.mei.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.ljyh.mei.ui.component.shimmer.skeleton

/** Shared typography for content sections; callers own the surrounding spacing. */
@Composable
fun MeiSectionTitle(text: String, modifier: Modifier = Modifier, isLoading: Boolean = false) {
    Text(
        text = if (isLoading) "正在加载" else text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.skeleton(isLoading),
    )
}

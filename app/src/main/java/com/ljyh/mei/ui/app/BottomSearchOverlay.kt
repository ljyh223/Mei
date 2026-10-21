package com.ljyh.mei.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.screen.search.SearchScreen

/**
 * Results layer for the bottom search field. It is drawn above the page and mini player,
 * while the bottom bar itself stays in the Scaffold bottomBar slot.
 */
@Composable
fun BottomSearchOverlay(
    active: Boolean,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: (String, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(enabled = active, onBack = onDismiss)
    AnimatedVisibility(
        visible = active,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    bottom = 96.dp,
                ),
        ) {
            SearchScreen(
                query = query.text,
                onQueryChange = onQueryChange,
                onSearch = onSubmit,
                onDismiss = onDismiss,
            )
        }
    }
}

package com.ljyh.mei.ui.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.LocalMeiGlassEnabled
import com.ljyh.mei.ui.component.layout.LocalPageStartInset
import com.ljyh.mei.ui.component.layout.meiPageGutter
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.ljyh.mei.ui.screen.search.SearchScreen
import com.ljyh.mei.ui.screen.search.SearchType
import com.ljyh.mei.ui.screen.main.library.LibrarySearchResults

/**
 * Results layer for the bottom search field. It is drawn above the page and mini player,
 * while the bottom bar itself stays in the Scaffold bottomBar slot.
 */
@Composable
fun BottomSearchOverlay(
    active: Boolean,
    librarySearch: Boolean = false,
    bottomBarClearance: Dp,
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    onSubmit: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState? = null,
    topSearchFocus: FocusRequester? = null,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = active, onBack = onDismiss)
    AnimatedVisibility(
        visible = active,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        // The parent already excludes the keyboard. Only reserve the measured bottom bar here.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (LocalMeiGlassEnabled.current && hazeState != null) Modifier.hazeSource(hazeState) else Modifier)
                .background(MaterialTheme.colorScheme.background)
                .then(if (topSearchFocus != null) Modifier.windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                ) else Modifier)
                .padding(
                    top = if (topSearchFocus != null) 8.dp
                    else WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    bottom = bottomBarClearance,
                    start = LocalPageStartInset.current,
                ),
        ) {
            if (topSearchFocus != null) {
                TextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter())
                        .focusRequester(topSearchFocus),
                    placeholder = { Text(if (librarySearch) "搜索歌单、专辑" else "搜索歌曲、歌手、专辑") },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    trailingIcon = { TextButton(onClick = onDismiss) { Text("取消") } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit(query.text, SearchType.All.type) }),
                )
            }
            if (librarySearch) LibrarySearchResults(query.text, onDismiss) else SearchScreen(
                modifier = Modifier.fillMaxSize(),
                showTitle = topSearchFocus == null,
                query = query.text,
                onQueryChange = onQueryChange,
                onSearch = onSubmit,
                onDismiss = onDismiss,
            )
        }
    }
}

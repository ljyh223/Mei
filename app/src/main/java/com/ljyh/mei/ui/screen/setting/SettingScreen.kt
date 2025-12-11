package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Kitesurfing
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.component.PreferenceEntry
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.Screen
import com.ljyh.mei.ui.screen.backToMain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val navController = LocalNavController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
        ) {


            PreferenceEntry(
                title = { Text("外观") },
                icon = { Icon(Icons.Rounded.Palette, contentDescription = null) },
                onClick = {
                    Screen.AppearanceSettings.navigate(navController)
                }
            )

            PreferenceEntry(
                title = { Text("内容") },
                icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
                onClick = {
                    Screen.ContentSettings.navigate(navController)
                }
            )
            PreferenceEntry(
                title = { Text("播放") },
                icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = null) },
                onClick = {
                    Screen.PlaySettings.navigate(navController)
                }
            )
        }
    }
}

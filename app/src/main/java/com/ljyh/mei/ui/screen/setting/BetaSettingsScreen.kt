package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.constants.CoverBackgroundEnabledKey
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.setting.component.CoverBackgroundSettings
import com.ljyh.mei.utils.preferences.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetaSettingsScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val navController = LocalNavController.current
    val (coverExtensionEnabled, setCoverExtensionEnabled) = rememberPreference(
        CoverBackgroundEnabledKey, defaultValue = false,
    )

    MeiPinnedPage(
        title = "Beta 功能",
        onNavigateBack = navController::navigateUp,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues)
        ) {
            MeiLargeTitle("Beta 功能")

            PreferenceSection("播放器") {
                SwitchPreference(
                    title = { Text("封面底色扩展") },
                    description = "仅适用于静态封面；识别到纯色底色且边缘适合延伸时启用",
                    icon = { Icon(Icons.Rounded.Image, contentDescription = null) },
                    checked = coverExtensionEnabled,
                    onCheckedChange = setCoverExtensionEnabled,
                )
            }
            if (coverExtensionEnabled) CoverBackgroundSettings()
        }
    }
}

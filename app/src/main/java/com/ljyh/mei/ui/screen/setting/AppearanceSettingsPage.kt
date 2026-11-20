package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalNavController

/** One settings page; wide layouts keep the existing category rail outside navigation. */
@Composable
internal fun AppearanceSettingsPage(title: String, content: @Composable ColumnScope.() -> Unit) {
    val navController = LocalNavController.current
    MeiPinnedPage(title = title, onNavigateBack = navController::navigateUp) { paddingValues ->
        Column(Modifier.fillMaxSize().verticalScroll(LocalMeiPageScroll.current!!).padding(paddingValues)) {
            MeiLargeTitle(title)
            content()
        }
    }
}

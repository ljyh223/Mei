package com.ljyh.mei.ui.app

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.screen.setting.PlaySettingScreen
import com.ljyh.mei.ui.screen.setting.SplitSettingsHost
import com.ljyh.mei.ui.screen.setting.isSettingsDetailTransition

@OptIn(ExperimentalMaterial3Api::class)
internal object SplitSettingsPageContent : MeiPageContent by SharedMeiPageContent {
    @Composable
    override fun Settings(scrollBehavior: TopAppBarScrollBehavior) {
        PlaySettingScreen(scrollBehavior)
    }

    override fun useImmediateTransition(navController: NavHostController, from: String?, to: String?): Boolean =
        navController.isSettingsDetailTransition(from, to)

    @Composable
    override fun PageHost(
        navController: NavHostController,
        modifier: Modifier,
        content: @Composable (Modifier) -> Unit,
    ) {
        SplitSettingsHost(navController, modifier, content)
    }
}

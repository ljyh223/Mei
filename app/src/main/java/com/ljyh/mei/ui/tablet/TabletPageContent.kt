package com.ljyh.mei.ui.tablet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.ljyh.mei.ui.app.MeiPageContent
import com.ljyh.mei.ui.app.SharedMeiPageContent

/** Wide-window settings use the shared routes and business owners. */
@OptIn(ExperimentalMaterial3Api::class)
internal object TabletPageContent : MeiPageContent by SharedMeiPageContent {
    @Composable override fun Settings(scrollBehavior: TopAppBarScrollBehavior) {
        TabletSettingsHome(scrollBehavior)
    }
    override fun useImmediateTransition(navController: NavHostController, from: String?, to: String?): Boolean =
        navController.isSettingsDetailTransition(from, to)

    @Composable override fun PageHost(navController: NavHostController, modifier: Modifier, content: @Composable (Modifier) -> Unit) {
        TabletSettingsHost(navController, modifier, content)
    }
}

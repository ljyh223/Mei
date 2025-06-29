package com.ljyh.mei.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.ljyh.mei.ui.local.LocalNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = WindowInsets.statusBars.asPaddingValues(),
    navigationIcon: @Composable () -> Unit ={},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = topAppBarColors(
    ),
    scrollBehavior: TopAppBarScrollBehavior? = null
) {

    val appBarContainerColor = MaterialTheme.colorScheme.primary

    Surface(modifier = modifier, color = appBarContainerColor) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior
        )
    }
}

@Composable
fun PopBackIcon(
    icon: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
){
    val navController = LocalNavController.current
    IconButton(onClick = {
        navController.popBackStack()
    }) {
        Icon(icon, "Back")
    }
}
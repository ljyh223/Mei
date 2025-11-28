package com.ljyh.mei.ui.model

import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)
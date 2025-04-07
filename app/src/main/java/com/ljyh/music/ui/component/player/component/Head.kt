package com.ljyh.music.ui.component.player.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp

@Composable
fun Head(
    title:String,
    subtitle:String
){
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 16.sp,
            maxLines = 1
        )
    }
}
package com.ljyh.music.ui.component.shimmer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardPlaceholder(){
    Row(
        modifier = Modifier.width(100.dp)
    ){
        TextPlaceholder(
            Modifier.size(100.dp, 100.dp)
                .padding(bottom = 8.dp, end = 8.dp)
        )
        TextPlaceholder()
    }
}
package com.ljyh.mei.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.shimmer.CardPlaceholder
import com.ljyh.mei.ui.component.shimmer.ShimmerHost
import com.ljyh.mei.ui.component.shimmer.TextPlaceholder

@Composable
fun HomeShimmer(){
    ShimmerHost{
        TextPlaceholder()
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            repeat(3){
                TextPlaceholder(
                    Modifier.size(120.dp)
                        .padding(end = 6.dp)
                )
            }
        }

        TextPlaceholder()
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            repeat(4){
                CardPlaceholder()
            }
        }
        TextPlaceholder()
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            repeat(4){
                CardPlaceholder()
            }
        }
    }
}
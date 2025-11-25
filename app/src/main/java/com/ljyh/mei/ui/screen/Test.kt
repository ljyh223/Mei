package com.ljyh.mei.ui.screen


import androidx.compose.foundation.border
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage


@Composable
fun Test2(){
    AsyncImage(
        model = "https://p5.music.126.net/obj/wonDlsKUwrLClGjCm8Kx/33013382162/9706/80e4/105a/d1ac01d5ee46dbcad97051f0197c8b61.jpg?imageView=1&thumbnail=500y500",
        contentDescription = null,
        modifier = Modifier
            .requiredSize(60.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White, RoundedCornerShape(16.dp))

    )
}
@Composable
fun Test(){
    Test2()

}
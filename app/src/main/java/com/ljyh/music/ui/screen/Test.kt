package com.ljyh.music.ui.screen


import android.graphics.Matrix
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ljyh.music.ui.component.player.component.animatedGradient


@Composable
fun animateBrushRotation(
    shader: Shader,
    size: Size,
    duration: Int,
    clockwise: Boolean
): State<ShaderBrush> {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f * if (clockwise) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    return remember(shader, size) {
        derivedStateOf {
            val matrix = Matrix().apply {
                postRotate(angle, size.width / 2, size.height / 2)
            }
            shader.setLocalMatrix(matrix)
            ShaderBrush(shader)
        }
    }
}

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
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = MaterialTheme.colorScheme.background
//    ) {
//        var size by remember { mutableStateOf(Size.Zero) }
//
//        val shaderA = LinearGradientShader(
//            Offset(size.width / 2f, 0f),
//            Offset(size.width / 2f, size.height),
//            listOf(
//                MaterialTheme.colorScheme.primaryContainer,           // 核心品牌色[6,7](@ref)
//                MaterialTheme.colorScheme.error   // 第三色浅调容器[4,6](@ref)
//            ),
//            listOf(0f, 1f)
//        )
//
//        val shaderB = LinearGradientShader(
//            Offset(size.width / 2f, 0f),
//            Offset(size.width / 2f, size.height),
//            listOf(
//                MaterialTheme.colorScheme.tertiaryContainer,     // 错误色浅调容器[3,7](@ref)
//                MaterialTheme.colorScheme.onSurfaceVariant    // 次色反色[3,6](@ref)
//            ),
//            listOf(0f, 1f)
//        )
//
//        val shaderMask = LinearGradientShader(
//            Offset(size.width / 2f, 0f),
//            Offset(size.width / 2f, size.height),
//            listOf(
//                Color.White,
//                Color.Transparent,
//            ),
//            listOf(0f, 1f)
//        )
//
//        val brushA by animateBrushRotation(shaderA, size, 20_000, true)
//        val brushB by animateBrushRotation(shaderB, size, 12_000, false)
//        val brushMask by animateBrushRotation(shaderMask, size, 15_000, true)
//
//        Box(
//            modifier = Modifier
//                .requiredSize(300.dp)
//                .onSizeChanged {
//                    size = Size(it.width.toFloat(), it.height.toFloat())
//                }
//                .clip(RoundedCornerShape(16.dp))
//                .border(1.dp, Color.White, RoundedCornerShape(16.dp))
//                .animatedGradient(true),
//            contentAlignment = Alignment.Center
//        ) {
//            Text(
//                modifier = Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
//                text = "FLUID",
//                style = MaterialTheme.typography.headlineLarge,
//                fontWeight = FontWeight.Light
//            )
//        }
//    }
}
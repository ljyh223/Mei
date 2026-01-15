package com.ljyh.mei.ui.component.playlist

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ljyh.mei.utils.UnitUtils.toPx
import timber.log.Timber
import kotlin.math.*

private const val ROTATION_DEGREES = -30f // 逆时针旋转30度


// 使用一个足够大的固定缩放值，确保内容远超视口大小，杜绝空白
private const val FIXED_SCALE = 1.8f

@Composable
fun FinalPerfectCollage(
    imageUrls: List<String>,
    modifier: Modifier = Modifier
) {
    // BoxWithConstraints 是实现此效果的完美工具。
    // 它为我们提供了父容器的尺寸，而无需复杂的 Layout 或 SubcomposeLayout。
    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        // maxWidth 和 maxHeight 就是我们最终要填充的正方形视口的尺寸
        val viewportWidth = this.maxWidth.value
        val viewportHeight = this.maxHeight.value

        // 由于 ImageCollageContentV2 使用了 fillMaxWidth 和 aspectRatio,
        // 我们可以直接计算出它的原始尺寸，而无需测量。
        // 这是此方案能够如此简洁的关键。
        val originalWidth = viewportWidth
        val spacing = 4.dp.toPx(
            context = LocalContext.current
        ) // 与 ImageCollageContentV2 中 verticalArrangement 的间距保持一致

        // 计算拼图原始高度
        val topRowHeight = originalWidth / 2f // 上面是2个1:1的图片
        val bottomRowHeight = (originalWidth / 4f) / 3f // 下面是4个slot，3:1的图片
        val originalHeight = topRowHeight + bottomRowHeight + spacing

        // --- 核心计算：偏移量 ---
        // 目标：将第一张图的中心，经过变换后，移动到视口的中心。

        // 1. 拼图的变换中心（默认是其几何中心）
        val collageCenterX = originalWidth / 2f
        val collageCenterY = originalHeight / 2f

        // 2. 我们要对齐的目标点：第一张图的中心
        val firstImageCenterX = originalWidth / 4f
        val firstImageCenterY = topRowHeight / 2f

        // 3. 计算目标点相对于变换中心的向量
        val vecX = firstImageCenterX - collageCenterX
        val vecY = firstImageCenterY - collageCenterY

        // 4. 对该向量应用缩放和旋转变换
        val angleRad = ROTATION_DEGREES * PI.toFloat() / 180f
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)

        // 注意：graphicsLayer 的变换顺序是 scale -> rotate -> translate
        val scaledVecX = vecX * FIXED_SCALE
        val scaledVecY = vecY * FIXED_SCALE

        val rotatedVecX = scaledVecX * cosA - scaledVecY * sinA
        val rotatedVecY = scaledVecX * sinA + scaledVecY * cosA

        // 5. 计算最终需要的平移量 (translation)
        // 我们需要施加一个平移，使得变换后的点 (collageCenter + rotatedVec) 被移动到视口中心 (viewportCenter)
        // translation = viewportCenter - (collageCenter + rotatedVec)
        val finalTranslationX = (viewportWidth / 2f) - (collageCenterX + rotatedVecX)
        val finalTranslationY = (viewportHeight / 2f) - (collageCenterY + rotatedVecY)

        Timber.tag("FinalCollageV6").d("Viewport: ${viewportWidth}x${viewportHeight}")
        Timber.tag("FinalCollageV6").d("Original Collage Size: ${originalWidth}x${originalHeight}")
        Timber.tag("FinalCollageV6").d("Translation X: $finalTranslationX, Y: $finalTranslationY")

        // 应用变换
        Box(
            modifier = Modifier
                .graphicsLayer {
                    // 应用我们计算出的平移、旋转和固定的缩放
                    translationX = finalTranslationX
                    translationY = finalTranslationY
                    rotationZ = ROTATION_DEGREES
                    scaleX = FIXED_SCALE
                    scaleY = FIXED_SCALE
                }
        ) {
            ImageCollageContent(imageUrls = imageUrls)
        }
    }
}
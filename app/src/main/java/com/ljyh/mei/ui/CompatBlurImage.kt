package com.ljyh.mei.ui


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope.Companion.DefaultFilterQuality
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlin.math.ceil

@Suppress("DEPRECATION")
fun blurBitmapWithRenderScript(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
    val rs = RenderScript.create(context)
    val input = Allocation.createFromBitmap(rs, bitmap)
    val output = Allocation.createTyped(rs, input.type)
    val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
    script.setRadius(radius.coerceIn(0f, 25f))
    script.setInput(input)
    script.forEach(output)
    val result = createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    output.copyTo(result)
    rs.destroy()
    return result
}

fun blurBitmapUnbounded(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
    val padding = ceil(radius.toDouble()).toInt()
    val newWidth = bitmap.width + padding * 2
    val newHeight = bitmap.height + padding * 2

    val paddedBitmap = createBitmap(newWidth, newHeight, bitmap.config ?: Bitmap.Config.ARGB_8888)

    val canvas = Canvas(paddedBitmap)
    canvas.drawBitmap(
        bitmap,
        padding.toFloat(), // left
        padding.toFloat(), // top
        null // paint
    )

    return blurBitmapWithRenderScript(context, paddedBitmap, radius)
}

@Composable
fun CompatBlurImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    blurRadius: Dp = 0.dp,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    filterQuality: FilterQuality = DefaultFilterQuality,
) {
    val context = LocalContext.current

    // 使用 Coil 的 AsyncImage 加载图片
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.dp) {
        // Android 13+ 使用内置模糊
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier.blur(blurRadius, BlurredEdgeTreatment.Unbounded),
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
            filterQuality = filterQuality
        )
    } else {
        // Android 12 及以下使用自定义模糊
        var bitmapState by remember { mutableStateOf<ImageBitmap?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        val density = LocalDensity.current
        LaunchedEffect(imageUrl, blurRadius) {
            if (blurRadius > 0.dp) {
                isLoading = true
                try {
                    val imageLoader = ImageLoader.Builder(context)
                        .build()

                    val request = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .allowHardware(false)
                        .build()

                    // 注意：execute 是 suspend 函数
                    val result = imageLoader.execute(request)

                    if (result is SuccessResult) {
                        val drawable = result.image
                        if (drawable is BitmapDrawable) {
                            val originalBitmap = drawable.bitmap
                            val blurRadiusPx = with(density) { blurRadius.toPx() }

                            val blurredBitmap = blurBitmapUnbounded(context, originalBitmap, blurRadiusPx)
                            bitmapState = blurredBitmap.asImageBitmap()
                        }
                    } else if (result is ErrorResult) {
                        println("Load failed: ${result.throwable}")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            } else {
                bitmapState = null
                isLoading = false
            }
        }

        if (isLoading) {
            // 加载中显示占位符
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                // 可以在这里添加加载动画或占位图
            }
        } else if (bitmapState != null && blurRadius > 0.dp) {
            // 显示模糊后的图片
            Image(
                painter = remember(bitmapState) {
                    BitmapPainter(bitmapState!!, filterQuality = filterQuality)
                },
                contentDescription = contentDescription,
                modifier = modifier,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
            )
        } else {
            // 不需要模糊或模糊失败时显示原图
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = modifier,
                alignment = alignment,
                contentScale = contentScale,
                alpha = alpha,
                colorFilter = colorFilter,
                filterQuality = filterQuality
            )
        }
    }
}
package com.ljyh.mei.ui.component.player.component

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.ljyh.mei.ui.component.player.component.mesh.MeshBackgroundView
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SPlayerFluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bass by audioVisualizerManager.bassValue.collectAsState()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        var meshView by remember { mutableStateOf<MeshBackgroundView?>(null) }

        LaunchedEffect(imageUrl) {
            if (imageUrl.isNullOrEmpty()) return@LaunchedEffect
            withContext(Dispatchers.IO) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(256)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.toBitmap()
                    meshView?.setAlbum(bitmap)
                }
            }
        }

        LaunchedEffect(bass) {
            meshView?.updateVolume(bass * 0.1f)
        }

        AndroidView(
            factory = { ctx ->
                MeshBackgroundView(ctx).also { meshView = it }
            },
            modifier = modifier.fillMaxSize()
        )
    } else {
        AppleMusicFluidBackground(
            imageUrl = imageUrl,
            modifier = modifier
        )
    }
}
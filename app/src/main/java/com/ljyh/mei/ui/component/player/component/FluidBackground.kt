package com.ljyh.mei.ui.component.player.component

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
import coil3.Bitmap
import com.ljyh.mei.constants.MeshFlowSpeedKey
import com.ljyh.mei.constants.MeshLowFreqVolumeKey
import com.ljyh.mei.constants.MeshPlayingKey
import com.ljyh.mei.constants.MeshRenderScaleKey
import com.ljyh.mei.constants.MeshStaticModeKey
import com.ljyh.mei.constants.MeshSubdivisionKey
import com.ljyh.mei.ui.component.player.component.mesh.MeshBackgroundView
import com.ljyh.mei.utils.audio.AudioVisualizerManager
import com.ljyh.mei.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FluidBackground(
    imageUrl: String?,
    audioVisualizerManager: AudioVisualizerManager,
    isPlaying: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bass by audioVisualizerManager.bassValue.collectAsState()

    val (flowSpeed) = rememberPreference(MeshFlowSpeedKey, defaultValue = 0.25f)
    val (renderScale) = rememberPreference(MeshRenderScaleKey, defaultValue = 0.75f)
    val (staticMode) = rememberPreference(MeshStaticModeKey, defaultValue = false)
    val (meshPlaying) = rememberPreference(MeshPlayingKey, defaultValue = true)
    val (volumeScale) = rememberPreference(MeshLowFreqVolumeKey, defaultValue = 0.1f)
    val (subdivision) = rememberPreference(MeshSubdivisionKey, defaultValue = 50)

    // 将图片加载逻辑独立出来，只负责把 Bitmap 提取出来
    val albumBitmap by produceState<Bitmap?>(null, imageUrl) {
        if (imageUrl.isNullOrEmpty()) {
            value = null
            return@produceState
        }
        withContext(Dispatchers.IO) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(256)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                value = result.image.toBitmap()
            }
        }
    }

    val shouldAnimate = !meshPlaying || isPlaying

    // 【关键修复】：记住上一次成功送入 View 的图片URL，防止高频低音信号引起的重组导致无限触发 view.setAlbum 从而撑爆显存
    var lastSubmittedUrl by remember { mutableStateOf<String?>(null) }

    AndroidView(
        factory = { ctx ->
            MeshBackgroundView(ctx).apply {
                setFlowSpeed(flowSpeed)
                setRenderScale(renderScale)
                setSubdivision(subdivision)
                setStaticMode(staticMode)
                setPlaying(shouldAnimate)
                setPreserveEGLContextOnPause(true)
            }
        },
        update = { view ->
            // 【关键修复】：只有在图片真正的 URL 改变且 Bitmap 加载好时，才允许重新在底层生成网格图层
            if (imageUrl != lastSubmittedUrl && albumBitmap != null) {
                view.setAlbum(albumBitmap!!)
                lastSubmittedUrl = imageUrl
            }

            view.updateVolume(bass * volumeScale)
            view.setFlowSpeed(flowSpeed)
            view.setRenderScale(renderScale)
            view.setSubdivision(subdivision)
            view.setStaticMode(staticMode)
            view.setPlaying(shouldAnimate)
        },
        modifier = modifier.fillMaxSize()
    )
}

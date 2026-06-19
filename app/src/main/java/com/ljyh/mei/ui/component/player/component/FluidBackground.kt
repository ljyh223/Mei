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

    // 【关键修复】：利用引用相等性纪录最后一次提交的 Bitmap，彻底拦截由 bass 节奏高频重组引起的 view.setAlbum 卡死 Bug
    var lastBitmap by remember { mutableStateOf<Bitmap?>(null) }

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
            // 只有当新封面加载完成且和旧封面不一致时，才提交给 OpenGL 生成全新过渡层
            if (albumBitmap != null && albumBitmap !== lastBitmap) {
                view.setAlbum(albumBitmap!!)
                lastBitmap = albumBitmap
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

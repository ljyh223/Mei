package com.ljyh.mei.ui.component.player.component

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
import android.graphics.Bitmap
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
                // 【终极核武修复 1】：彻底抹杀 Android 16 偷偷返回 Hardware Bitmap 的可能。
                // 强行拷贝一份纯软件 ARGB_8888 内存图，这是唯一能让天玑 GPU 安全读取出像素而不是纯黑的方案！
                val rawBmp = result.image.toBitmap()
                value = rawBmp.copy(Bitmap.Config.ARGB_8888, false)
            }
        }
    }

    val shouldAnimate = !meshPlaying || isPlaying

    // 高频解耦，防止 UI 卡死
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

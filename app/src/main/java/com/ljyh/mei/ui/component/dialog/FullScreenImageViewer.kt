package com.ljyh.mei.ui.component.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ljyh.mei.utils.image.saveImageToGallery
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.ljyh.mei.ui.component.glass.MeiGlassIconButton
import com.ljyh.mei.ui.component.layout.meiPageGutter
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

@Composable
fun FullScreenImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            val source = rememberHazeState()
            Box(
                Modifier.fillMaxSize().background(Color.Black)
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) },
            ) {
                Box(Modifier.fillMaxSize().hazeSource(source).background(Color.Black)) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "封面预览",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                            .zoomable(rememberZoomState())
                            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) },
                    )
                }
                Box(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = meiPageGutter() - 4.dp, vertical = 16.dp)
                        .align(Alignment.TopCenter),
                ) {
                    MeiGlassIconButton(onClick = onDismiss, hazeState = source, modifier = Modifier.align(Alignment.TopStart)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭预览", tint = Color.White)
                    }
                }
                Box(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = meiPageGutter() - 4.dp, vertical = 16.dp)
                        .align(Alignment.BottomCenter),
                ) {
                    MeiGlassIconButton(
                        hazeState = source,
                        onClick = { scope.launch { saveImageToGallery(context, imageUrl) } },
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = "保存封面", tint = Color.White)
                    }
                }
            }
        }
    }
}

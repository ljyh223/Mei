package com.ljyh.mei.demo.album

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.ljyh.mei.R
import com.ljyh.mei.ui.component.layout.meiPageGutter
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt

internal enum class AlbumDemoFormat(val label: String, val objectName: String) {
    CD("CD", "CD 盒"), Cassette("磁带", "磁带"), Vinyl("黑胶", "黑胶唱片"),
}

@Composable
internal fun Album3dDemoScreen(onClose: () -> Unit, initialFormat: AlbumDemoFormat = AlbumDemoFormat.CD) {
    var selected by rememberSaveable { mutableStateOf(initialFormat.name) }
    val savedVersions = rememberSaveableStateHolder()
    val format = AlbumDemoFormat.valueOf(selected)
    savedVersions.SaveableStateProvider(format.name) {
        Album3dDemoContent(format, onClose, onFormatChange = { selected = it.name })
    }
}

@Composable
private fun Album3dDemoContent(
    format: AlbumDemoFormat,
    onClose: () -> Unit,
    onFormatChange: (AlbumDemoFormat) -> Unit,
) {
    var yaw by rememberSaveable { mutableFloatStateOf(-16f) }
    var pitch by rememberSaveable { mutableFloatStateOf(-8f) }
    var detail by rememberSaveable { mutableFloatStateOf(0f) }
    var mechanism by rememberSaveable { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var adjustingDetail by remember { mutableStateOf(false) }
    val renderedYaw by animateFloatAsState(yaw, tween(if (dragging) 0 else 350), label = "media rotation")
    val renderedPitch by animateFloatAsState(pitch, tween(if (dragging) 0 else 350), label = "media tilt")
    val detailDuration = if (format == AlbumDemoFormat.Cassette) 700 else 400
    val renderedDetail by animateFloatAsState(detail, tween(if (adjustingDetail) 0 else detailDuration), label = "media detail")
    val renderedMechanism by animateFloatAsState(mechanism, tween(1000, easing = LinearOutSlowInEasing), label = "media mechanism")
    val cover = rememberAsyncImagePainter(R.drawable.album_demo_cover)
    val cd = remember { AlbumCaseRenderer() }
    val cassette = remember { CassetteDemoRenderer() }
    val vinyl = remember { VinylDemoRenderer() }
    val gutter = meiPageGutter()
    val expanded = detail > if (format == AlbumDemoFormat.CD) 80f else .5f
    val primaryLabel = when (format) {
        AlbumDemoFormat.CD -> if (expanded) "合上盒盖" else "打开盒盖"
        AlbumDemoFormat.Cassette -> when {
            detail < .22f -> "打开包装"
            detail < .73f -> "取出磁带"
            else -> "装回包装"
        }
        AlbumDemoFormat.Vinyl -> if (expanded) "收回唱片" else "抽出唱片"
    }
    val detailLabel = when (format) {
        AlbumDemoFormat.CD -> "盒盖"
        AlbumDemoFormat.Cassette -> "展开进度"
        AlbumDemoFormat.Vinyl -> "抽出"
    }
    fun activate() {
        when (format) {
            AlbumDemoFormat.CD -> detail = if (expanded) 0f else 155f
            AlbumDemoFormat.Cassette -> detail = when {
                detail < .22f -> CassettePackageOpenProgress
                detail < .73f -> 1f
                else -> 0f
            }
            AlbumDemoFormat.Vinyl -> detail = if (expanded) 0f else 1f
        }
    }
    fun reset() {
        yaw = round(yaw / 360f) * 360f - 16f
        pitch = -8f
        detail = 0f
        mechanism = 0f
    }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2918), MaterialTheme.colorScheme.background)))
            .safeDrawingPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "关闭 Demo")
            }
            Text("专辑 3D", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = ::reset) {
                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("复位")
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = gutter).heightIn(min = 48.dp)) {
            AlbumDemoFormat.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = format == option,
                    onClick = { onFormatChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, AlbumDemoFormat.entries.size),
                ) { Text(option.label) }
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val landscape = maxWidth > maxHeight && maxWidth >= 600.dp
            val stageHeight = (maxWidth * .92f).coerceIn(240.dp, 430.dp)
            val artwork: @Composable (Modifier) -> Unit = { modifier ->
                Canvas(
                    modifier
                        .clickable(onClickLabel = primaryLabel, onClick = ::activate)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    yaw = renderedYaw
                                    pitch = renderedPitch
                                    dragging = true
                                },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                            ) { change, delta ->
                                change.consume()
                                yaw += delta.x / size.width.coerceAtLeast(1) * 360f
                                pitch = (pitch - delta.y / size.height.coerceAtLeast(1) * 100f).coerceIn(-50f, 50f)
                            }
                        }
                        .semantics {
                            contentDescription = "Armageddon ${format.objectName}，可拖动旋转"
                            stateDescription = when (format) {
                                AlbumDemoFormat.CD -> if (expanded) "盒盖已打开" else "盒盖已合上"
                                AlbumDemoFormat.Cassette -> when {
                                    renderedDetail < .01f -> "磁带包装已合上"
                                    renderedDetail < .44f -> "包装正在开合"
                                    renderedDetail < .46f -> "包装已打开，磁带在托盘内"
                                    renderedDetail < .99f -> if (detail > renderedDetail) "正在取出磁带" else "正在收纳磁带"
                                    cos(radians(renderedYaw)) < 0f -> "磁带已取出，B 面"
                                    else -> "磁带已取出，A 面"
                                }
                                AlbumDemoFormat.Vinyl -> if (expanded) "唱片已抽出" else "唱片在封套内"
                            }
                            customActions = listOf(
                                CustomAccessibilityAction("向上倾斜") { pitch = (pitch + 10f).coerceAtMost(50f); true },
                                CustomAccessibilityAction("向下倾斜") { pitch = (pitch - 10f).coerceAtLeast(-50f); true },
                            )
                        },
                ) {
                    when (format) {
                        AlbumDemoFormat.CD -> cd.draw(this, cover, renderedYaw, renderedPitch, renderedDetail)
                        AlbumDemoFormat.Cassette -> cassette.draw(this, cover, renderedYaw, renderedPitch,
                            renderedDetail, renderedMechanism)
                        AlbumDemoFormat.Vinyl -> vinyl.draw(this, cover, renderedYaw, renderedPitch,
                            renderedDetail, renderedMechanism)
                    }
                }
            }
            val controls: @Composable (Modifier) -> Unit = { modifier ->
                Column(
                    modifier.padding(horizontal = gutter, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Armageddon", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("aespa · ${format.label}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledTonalButton(onClick = ::activate, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Text(primaryLabel, textAlign = TextAlign.Center)
                        }
                        OutlinedButton(onClick = { yaw += 90f }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.RotateRight, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("旋转", textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(detailLabel, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text(when (format) {
                            AlbumDemoFormat.CD -> "${detail.roundToInt()}°"
                            AlbumDemoFormat.Cassette -> "${(detail * 100).roundToInt()}%"
                            AlbumDemoFormat.Vinyl -> "${(detail * 100).roundToInt()}%"
                        }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = detail,
                        onValueChange = { adjustingDetail = true; detail = it },
                        onValueChangeFinished = { adjustingDetail = false },
                        valueRange = if (format == AlbumDemoFormat.CD) 0f..160f else 0f..1f,
                        modifier = Modifier.semantics { contentDescription = when (format) {
                            AlbumDemoFormat.CD -> "盒盖开合角度"
                            AlbumDemoFormat.Cassette -> "打开包装与取出磁带的进度"
                            AlbumDemoFormat.Vinyl -> "唱片抽出比例"
                        } },
                    )
                    when (format) {
                        AlbumDemoFormat.Cassette -> FlowRow(
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(onClick = { yaw += 180f }) { Text("翻面") }
                            TextButton(onClick = { mechanism += 360f }, enabled = renderedDetail >= .99f) {
                                Text("转动带盘")
                            }
                            TextButton(onClick = { detail = 0f }, enabled = detail > .001f) { Text("合上包装") }
                        }
                        AlbumDemoFormat.Vinyl -> TextButton(onClick = { mechanism += 360f }) { Text("转动唱片") }
                        AlbumDemoFormat.CD -> Unit
                    }
                    Text(when (format) {
                        AlbumDemoFormat.CD -> "拖动专辑旋转 · 点击开合"
                        AlbumDemoFormat.Cassette -> "拖动旋转 · 点击开盒、取出磁带"
                        AlbumDemoFormat.Vinyl -> "拖动黑胶旋转 · 点击抽出唱片"
                    }, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }
            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    artwork(Modifier.weight(1.2f).fillMaxHeight())
                    controls(Modifier.weight(1f).verticalScroll(rememberScrollState()))
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    artwork(Modifier.fillMaxWidth().height(stageHeight))
                    controls(Modifier.widthIn(max = 480.dp).fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

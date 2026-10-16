package com.ljyh.mei.ui.component.player.component.sheet

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.core.content.ContextCompat
import com.ljyh.mei.playback.PlayerConnection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    playerConnection: PlayerConnection,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val service = playerConnection.service
    var mode by remember {
        mutableStateOf(if (service.sleepTimer.sleepDetectionActive) SleepStopMode.Detection else SleepStopMode.Countdown)
    }
    var countdownMinutes by remember { mutableIntStateOf(30) }
    var detectionLimitMinutes by remember {
        val timer = service.sleepTimer
        val remainingMinutes = ((timer.triggerTime - System.currentTimeMillis() + 59_999L) / 60_000L).toInt()
        mutableIntStateOf(if (timer.sleepDetectionActive) remainingMinutes.coerceIn(30, 240) else 120)
    }
    var startingDetection by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var closed by remember { mutableStateOf(false) }
    val selectedMinutes = if (mode == SleepStopMode.Countdown) countdownMinutes else detectionLimitMinutes
    val minimumMinutes = if (mode == SleepStopMode.Countdown) 1 else 30

    fun setMinutes(minutes: Int) {
        if (mode == SleepStopMode.Countdown) countdownMinutes = minutes.coerceIn(1, 240)
        else detectionLimitMinutes = minutes.coerceIn(30, 240)
    }

    fun dismissSheet() {
        closed = true
        service.sleepDetectionController.cancelPending()
        onDismiss()
    }

    fun startDetection() {
        if (closed) return
        startingDetection = true
        errorMessage = null
        service.sleepDetectionController.start(detectionLimitMinutes) { error ->
            startingDetection = false
            if (error == null) dismissSheet() else errorMessage = error
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (closed) return@rememberLauncherForActivityResult
        if (granted) startDetection() else errorMessage = "需要活动识别权限才能检测入睡"
    }

    ModalBottomSheet(
        onDismissRequest = ::dismissSheet,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "睡眠定时",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            val timer = service.sleepTimer
            if (timer.isActive) {
                Text(
                    text = when {
                        timer.pauseWhenSongEnd -> "当前设置：本曲结束后停止"
                        timer.sleepDetectionActive -> "入睡检测进行中 · 最晚 ${formatStopTime(timer.triggerTime)} 暂停"
                        else -> "当前计时中 · ${formatStopTime(timer.triggerTime)} 暂停"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SleepModeButton(
                    text = "定时停止",
                    selected = mode == SleepStopMode.Countdown,
                    modifier = Modifier.weight(1f),
                ) { mode = SleepStopMode.Countdown; errorMessage = null }
                SleepModeButton(
                    text = "入睡后停止",
                    selected = mode == SleepStopMode.Detection,
                    modifier = Modifier.weight(1f),
                ) { mode = SleepStopMode.Detection; errorMessage = null }
            }

            val endTime = System.currentTimeMillis() + selectedMinutes * 60_000L
            Text(
                text = if (mode == SleepStopMode.Countdown) {
                    "预计 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))} 停止播放"
                } else {
                    "检测到入睡时暂停，最晚 ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))} 停止"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (mode == SleepStopMode.Detection) {
                Text(
                    text = "由 Google Play 服务检测，结果通常约每 10 分钟更新",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            val presets = if (mode == SleepStopMode.Countdown) listOf(5, 10, 15, 30, 45, 60)
            else listOf(30, 60, 90, 120, 180, 240)
            presets.chunked(3).forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { minutes ->
                        val isSelected = selectedMinutes == minutes
                        Surface(
                            onClick = { setMinutes(minutes) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .semantics { selected = isSelected },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = formatSleepDuration(minutes),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    if (mode == SleepStopMode.Countdown) "自定义时长" else "最长播放时间",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = formatSleepDuration(selectedMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { setMinutes(selectedMinutes - 1) },
                    enabled = selectedMinutes > minimumMinutes,
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Rounded.Remove, contentDescription = "减少 1 分钟") }
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { setMinutes(it.roundToInt()) },
                    valueRange = minimumMinutes.toFloat()..240f,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { stateDescription = formatSleepDuration(selectedMinutes) },
                )
                IconButton(
                    onClick = { setMinutes(selectedMinutes + 1) },
                    enabled = selectedMinutes < 240,
                    modifier = Modifier.size(48.dp),
                ) { Icon(Icons.Rounded.Add, contentDescription = "增加 1 分钟") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("$minimumMinutes 分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("4 小时", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !startingDetection) {
                        service.sleepTimer.start(-1)
                        dismissSheet()
                    }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null)
                Text("播放完当前歌曲后停止", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
            if (errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = {
                    if (mode == SleepStopMode.Countdown) {
                        service.sleepTimer.start(countdownMinutes)
                        dismissSheet()
                    } else {
                        val availabilityError = service.sleepDetectionController.availabilityError()
                        when {
                            availabilityError != null -> errorMessage = availabilityError
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED -> startDetection()
                            else -> permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                    }
                },
                enabled = !startingDetection,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    when {
                        startingDetection -> "正在开启…"
                        mode == SleepStopMode.Detection && timer.sleepDetectionActive -> "更新最长时间"
                        mode == SleepStopMode.Detection -> "开启入睡检测"
                        else -> "开始计时"
                    }
                )
            }
            if (timer.isActive) {
                TextButton(
                    onClick = {
                        timer.clear()
                        dismissSheet()
                    },
                    enabled = !startingDetection,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("取消当前定时") }
            }
        }
    }
}

private enum class SleepStopMode { Countdown, Detection }

@Composable
private fun SleepModeButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
        }
    }
}

private fun formatSleepDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes 分钟"
    minutes % 60 == 0 -> "${minutes / 60} 小时"
    else -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
}

private fun formatStopTime(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestampMillis))

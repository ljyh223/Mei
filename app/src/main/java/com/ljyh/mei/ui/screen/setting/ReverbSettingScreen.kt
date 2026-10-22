package com.ljyh.mei.ui.screen.setting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.ReverbProfileKey
import com.ljyh.mei.playback.equalizer.ReverbPresets
import com.ljyh.mei.playback.equalizer.ReverbProfile
import com.ljyh.mei.playback.equalizer.ReverbProfileCodec
import com.ljyh.mei.ui.component.IconButton
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.local.LocalPlayerAwareWindowInsets
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReverbSettingScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(ReverbProfile.Off) }
    val committed = remember { mutableStateOf(ReverbProfile.Off) }

    LaunchedEffect(context) {
        context.dataStore.data.map { it[ReverbProfileKey] }
            .distinctUntilChanged()
            .collect { encoded ->
                val restored = ReverbProfileCodec.decode(encoded) ?: ReverbProfile.Off
                profile = restored
                committed.value = restored
            }
    }

    fun commit(next: ReverbProfile) {
        val normalized = next.normalized()
        profile = normalized
        committed.value = normalized
        playerConnection?.service?.previewReverbProfile(normalized)
        scope.launch {
            context.dataStore.edit { it[ReverbProfileKey] = ReverbProfileCodec.encode(normalized) }
        }
    }

    // Live preview while dragging; the sink keeps the last saved profile after leaving.
    fun preview(next: ReverbProfile) {
        profile = next.normalized()
        playerConnection?.service?.previewReverbProfile(profile)
    }

    DisposableEffect(playerConnection) {
        onDispose { playerConnection?.service?.previewReverbProfile(committed.value) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("混响") },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Switch(
                        checked = profile.enabled,
                        onCheckedChange = { commit(profile.copy(enabled = it)) },
                        modifier = Modifier.padding(end = 16.dp)
                            .semantics { contentDescription = "启用混响" },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .padding(paddingValues)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    )
                )
                .verticalScroll(rememberScrollState()),
        ) {
            PreferenceSection("房间预设") {
                Row(
                    Modifier.fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ReverbPresets.all.forEach { preset ->
                        FilterChip(
                            selected = profile.presetId == preset.id,
                            onClick = { commit(preset.profile) },
                            label = { Text(preset.label, maxLines = 1, softWrap = false) },
                        )
                    }
                }
            }
            PreferenceSection("空间") {
                ReverbSlider(
                    title = "房间大小",
                    description = "越大混响尾巴越长",
                    value = profile.roomSize,
                    onValueChange = { preview(profile.copy(roomSize = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
                ReverbSlider(
                    title = "阻尼",
                    description = "越大高频衰减越快，尾巴更暗",
                    value = profile.damping,
                    onValueChange = { preview(profile.copy(damping = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
                ReverbSlider(
                    title = "立体声宽度",
                    description = "0 为单声道混响，1 为完全展宽",
                    value = profile.width,
                    onValueChange = { preview(profile.copy(width = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
                ReverbSlider(
                    title = "预延迟 (ms)",
                    description = "直达声与混响之间的间隔",
                    value = profile.preDelayMs,
                    range = 0f..100f,
                    percent = false,
                    onValueChange = { preview(profile.copy(preDelayMs = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
            }
            PreferenceSection("电平") {
                ReverbSlider(
                    title = "湿声",
                    description = "混响信号比例",
                    value = profile.wet,
                    onValueChange = { preview(profile.copy(wet = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
                ReverbSlider(
                    title = "干声",
                    description = "原始信号比例",
                    value = profile.dry,
                    onValueChange = { preview(profile.copy(dry = it, presetId = "custom")) },
                    onFinished = { commit(profile) },
                )
            }
        }
    }
}

@Composable
private fun ReverbSlider(
    title: String,
    description: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    percent: Boolean = true,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (percent) "${(value * 100).roundToInt()}%" else value.roundToInt().toString(),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = title },
        )
    }
}

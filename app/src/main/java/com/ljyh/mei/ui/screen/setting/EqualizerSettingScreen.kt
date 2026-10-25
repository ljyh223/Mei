package com.ljyh.mei.ui.screen.setting

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.ljyh.mei.playback.equalizer.AutoEqFormat
import kotlinx.coroutines.flow.first
import com.ljyh.mei.playback.equalizer.TuningBundleCodec
import com.ljyh.mei.playback.equalizer.TuningBundle
import com.ljyh.mei.playback.equalizer.ReverbProfileCodec
import com.ljyh.mei.playback.equalizer.ReverbProfile
import com.ljyh.mei.constants.ReverbProfileKey
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
import android.net.Uri
import android.content.ClipboardManager
import android.content.ClipData
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.ljyh.mei.constants.EqualizerBandLevelsKey
import com.ljyh.mei.constants.EqualizerEnabledKey
import com.ljyh.mei.constants.OutputLimiterEnabledKey
import com.ljyh.mei.constants.ParametricEqualizerProfileKey
import com.ljyh.mei.constants.UserEqualizerPresetsKey
import com.ljyh.mei.playback.equalizer.AudioEffectsController
import com.ljyh.mei.playback.equalizer.BiquadDesign
import com.ljyh.mei.playback.equalizer.EqFilter
import com.ljyh.mei.playback.equalizer.EqualizerPreset
import com.ljyh.mei.playback.equalizer.EqualizerPresets
import com.ljyh.mei.playback.equalizer.EqualizerProfile
import com.ljyh.mei.playback.equalizer.EqualizerProfileCodec
import com.ljyh.mei.playback.equalizer.FilterType
import com.ljyh.mei.ui.local.LocalNavController
import androidx.compose.foundation.layout.fillMaxSize
import com.ljyh.mei.ui.component.glass.LocalMeiPageScroll
import com.ljyh.mei.ui.component.glass.MeiLargeTitle
import com.ljyh.mei.ui.component.glass.MeiPinnedPage
import com.ljyh.mei.ui.local.LocalPlayerConnection
import com.ljyh.mei.ui.screen.backToMain
import com.ljyh.mei.utils.preferences.dataStore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.ceil
import com.ljyh.mei.ui.component.layout.meiPageGutter
import com.ljyh.mei.ui.component.MeiFilterPill
import com.ljyh.mei.ui.screen.setting.component.PreferenceSection
import com.ljyh.mei.ui.screen.setting.component.SwitchPreference
import androidx.compose.ui.semantics.Role

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSettingScreen(scrollBehavior: TopAppBarScrollBehavior) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val playerConnection = LocalPlayerConnection.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf(EqualizerProfile()) }
    val committedProfile = remember { mutableStateOf(EqualizerProfile()) }
    var selectedId by remember { mutableIntStateOf(-1) }
    var tab by remember { mutableIntStateOf(0) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var deleteFilterId by remember { mutableStateOf<Int?>(null) }
    var deletePresetId by remember { mutableStateOf<String?>(null) }
    var customPresets by remember { mutableStateOf(emptyList<EqualizerPreset>()) }
    var bulkMode by remember { mutableStateOf(false) }
    var bulkSelection by remember { mutableStateOf(emptySet<Int>()) }
    var bulkGain by remember { mutableStateOf(0f) }
    var limiterEnabled by remember { mutableStateOf(true) }
    var showBundleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        launch {
            context.dataStore.data.map { prefs ->
                Triple(
                    prefs[ParametricEqualizerProfileKey],
                    prefs[EqualizerEnabledKey],
                    prefs[EqualizerBandLevelsKey]
                )
            }.distinctUntilChanged().collectLatest { (encoded, legacyEnabled, legacyLevels) ->
                val restored = EqualizerProfileCodec.decode(encoded)
                    ?: if (legacyLevels != null) AudioEffectsController.legacyProfile(
                        legacyEnabled ?: false, legacyLevels
                    )
                    else EqualizerProfile()
                profile = restored
                committedProfile.value = restored
            }
        }
        launch {
            context.dataStore.data.map { it[UserEqualizerPresetsKey].orEmpty() }
                .distinctUntilChanged()
                .collectLatest { customPresets = EqualizerPresets.decodeCustom(it) }
        }
        launch {
            context.dataStore.data.map { it[OutputLimiterEnabledKey] ?: true }
                .distinctUntilChanged()
                .collectLatest { limiterEnabled = it }
        }
    }

    fun commit(next: EqualizerProfile) {
        val normalized = next.normalized()
        profile = normalized
        committedProfile.value = normalized
        playerConnection?.service?.previewEqualizerProfile(normalized)
        scope.launch {
            context.dataStore.edit {
                it[ParametricEqualizerProfileKey] = EqualizerProfileCodec.encode(normalized)
            }
        }
    }

    DisposableEffect(playerConnection) {
        onDispose { playerConnection?.service?.previewEqualizerProfile(committedProfile.value) }
    }

    fun changeFilter(id: Int, transform: (EqFilter) -> EqFilter, save: Boolean) {
        val next = profile.copy(
            presetId = "custom",
            filters = profile.filters.map { if (it.id == id) transform(it).normalized() else it },
        )
        if (save) commit(next) else {
            profile = next
            playerConnection?.service?.previewEqualizerProfile(next)
        }
    }

    fun commitBulk(transform: (EqFilter) -> EqFilter) {
        val next = profile.copy(
            presetId = "custom",
            filters = profile.filters.map { if (it.id in bulkSelection) transform(it) else it },
        )
        commit(next)
    }

    fun autoPreamp() {
        val peakBoost = profile.filters.filter { it.enabled && it.type.usesGain }
            .maxOfOrNull { it.gainDb } ?: 0f
        if (peakBoost <= 0f) {
            Toast.makeText(context, "当前没有正增益频段", Toast.LENGTH_SHORT).show()
            return
        }
        commit(profile.copy(presetId = "custom", inputGainDb = (-peakBoost).coerceIn(-24f, 12f)))
        Toast.makeText(context, "前级已设为 -${peakBoost.roundToInt()} dB", Toast.LENGTH_SHORT).show()
    }

    fun importAutoEq(uri: Uri) {
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val parsed = text?.let { AutoEqFormat.parse(it) }
            if (parsed == null || parsed.filters.isEmpty()) {
                Toast.makeText(context, "无法解析 AutoEQ 文件", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val id = "user_autoeq_${System.currentTimeMillis()}"
            val name = uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() } ?: "AutoEQ"
            val preset = EqualizerPreset(id, name, parsed.copy(presetId = id))
            customPresets = customPresets + preset
            context.dataStore.edit {
                it[UserEqualizerPresetsKey] = EqualizerPresets.encodeCustom(customPresets)
            }
            commit(parsed.copy(presetId = id, enabled = true))
            selectedId = parsed.filters.firstOrNull()?.id ?: -1
            Toast.makeText(context, "已导入 ${parsed.filters.size} 段", Toast.LENGTH_SHORT).show()
        }
    }
    val autoEqPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importAutoEq(uri)
    }


    val selected = profile.filters.firstOrNull { it.id == selectedId }
    val currentPreset =
        (EqualizerPresets.builtIn + customPresets).firstOrNull { it.id == profile.presetId }

    fun exportBundle(uri: Uri) {
        scope.launch {
            val reverb = ReverbProfileCodec.decode(
                context.dataStore.data.first()[ReverbProfileKey]
            ) ?: ReverbProfile.Off
            val bundle = TuningBundle(currentPreset?.name ?: "Mei 调音", profile, reverb)
            val text = TuningBundleCodec.encode(bundle)
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(text.toByteArray(Charsets.UTF_8))
                    } != null
                }.getOrDefault(false)
            }
            Toast.makeText(
                context,
                if (ok) "调音已导出" else "导出失败",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    fun importBundle(uri: Uri) {
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            val bundle = text?.let(TuningBundleCodec::decode)
            if (bundle == null) {
                Toast.makeText(context, "不是有效的调音文件", Toast.LENGTH_SHORT).show()
                return@launch
            }
            context.dataStore.edit {
                it[ReverbProfileKey] = ReverbProfileCodec.encode(bundle.reverb)
            }
            playerConnection?.service?.previewReverbProfile(bundle.reverb)
            commit(bundle.eq.copy(enabled = true))
            Toast.makeText(context, "已导入「${bundle.name}」", Toast.LENGTH_SHORT).show()
        }
    }

    val bundleExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) exportBundle(uri) }
    val bundleImportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) importBundle(uri) }
    val response = remember(profile) { BiquadDesign.sampleResponse(profile) }
    val peakDb = response.maxOrNull()?.toFloat() ?: 0f
    LaunchedEffect(profile.filters.map(EqFilter::id)) {
        if (profile.filters.none { it.id == selectedId }) {
            selectedId = profile.filters.firstOrNull()?.id ?: -1
        }
    }

    MeiPinnedPage(
        title = "参数均衡器",
        onNavigateBack = navController::navigateUp,
        onBackLongClick = navController::backToMain,
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(LocalMeiPageScroll.current!!)
                .padding(paddingValues),
        ) {
            MeiLargeTitle("参数均衡器")
            PreferenceSection("音效") {
                SwitchPreference(
                    title = { Text("启用均衡器") },
                    description = currentPreset?.name ?: "自定义",
                    icon = { Icon(Icons.Rounded.Equalizer, contentDescription = null) },
                    checked = profile.enabled,
                    onCheckedChange = { commit(profile.copy(enabled = it)) },
                )
            }
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = meiPageGutter(), vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MeiFilterPill(
                    label = "编辑", selected = tab == 0, onClick = { tab = 0 },
                    role = Role.Tab, compact = true,
                )
                MeiFilterPill(
                    label = "内置与我的预设", selected = tab == 1, onClick = { tab = 1 },
                    role = Role.Tab, compact = true,
                )
            }

            if (tab == 0) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = meiPageGutter(), vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    EqualizerGraph(
                        profile = profile,
                        response = response,
                        selectedId = selectedId,
                        onSelect = { selectedId = it },
                        onFilterDrag = { id, frequency, gain, done ->
                            changeFilter(id, { filter ->
                                filter.copy(
                                    frequencyHz = frequency,
                                    gainDb = if (filter.type.usesGain) gain else filter.gainDb
                                )
                            }, done)
                        },
                    )
                }
                if (profile.enabled && peakDb > 0.5f) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = meiPageGutter()),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "峰值 +${formatNumber(peakDb, 1)} dB，可能削波",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val reduction = ceil(peakDb * 2f) / 2f
                            commit(
                                profile.copy(
                                    presetId = "custom", inputGainDb =
                                        (profile.inputGainDb - reduction).coerceAtLeast(-24f)
                                )
                            )
                        }) { Text("自动留余量") }
                    }
                }
                PreferenceSection("输出保护") {
                    SwitchPreference(
                        title = { Text("输出限幅") },
                        description = "提升频段后用软限幅防止削波",
                        checked = limiterEnabled,
                        onCheckedChange = { enabled ->
                            limiterEnabled = enabled
                            scope.launch {
                                context.dataStore.edit { it[OutputLimiterEnabledKey] = enabled }
                            }
                        },
                    )
                }
                PreferenceSection("电平") {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        ParameterSlider(
                            "输入增益",
                            profile.inputGainDb,
                            -24f..12f,
                            "dB",
                            1
                        ) { value, done ->
                            val next = profile.copy(presetId = "custom", inputGainDb = value)
                            if (done) commit(next) else {
                                profile = next
                                playerConnection?.service?.previewEqualizerProfile(next)
                            }
                        }
                        ParameterSlider(
                            "输出增益",
                            profile.outputGainDb,
                            -24f..12f,
                            "dB",
                            1
                        ) { value, done ->
                            val next = profile.copy(presetId = "custom", outputGainDb = value)
                            if (done) commit(next) else {
                                profile = next
                                playerConnection?.service?.previewEqualizerProfile(next)
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = meiPageGutter(), vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${profile.filters.size}/${EqualizerProfile.MAX_FILTERS} · 点选节点",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { showSaveDialog = true }) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(4.dp))
                        Text("保存")
                    }
                    Button(
                        enabled = profile.filters.size < EqualizerProfile.MAX_FILTERS,
                        onClick = {
                            val id = (profile.filters.maxOfOrNull(EqFilter::id) ?: 0) + 1
                            commit(
                                profile.copy(
                                    enabled = true,
                                    presetId = "custom",
                                    filters = profile.filters + EqFilter(
                                        id,
                                        FilterType.PEAK,
                                        1_000f
                                    ),
                                )
                            )
                            selectedId = id
                        },
                    ) { Icon(Icons.Rounded.Add, null); Text("添加") }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = meiPageGutter()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(onClick = { autoEqPicker.launch(arrayOf("*/*")) }) {
                        Text("导入曲线")
                    }
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("AutoEQ", AutoEqFormat.export(profile))
                        )
                        Toast.makeText(context, "曲线已复制", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("导出曲线")
                    }
                    TextButton(onClick = { showBundleDialog = true }) { Text("整套调音") }
                    TextButton(onClick = {
                        bulkMode = !bulkMode
                        if (!bulkMode) bulkSelection = emptySet()
                    }) {
                        Text(if (bulkMode) "完成" else "批量")
                    }
                }

                if (bulkMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = meiPageGutter(), vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("已选 ${bulkSelection.size} 段", modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    bulkSelection = profile.filters.map(EqFilter::id).toSet()
                                }) { Text("全选") }
                                TextButton(onClick = { bulkSelection = emptySet() }) { Text("清空") }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("批量增益", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = bulkGain,
                                    onValueChange = { value ->
                                        bulkGain = value
                                        if (bulkSelection.isNotEmpty()) {
                                            profile = profile.copy(
                                                presetId = "custom",
                                                filters = profile.filters.map {
                                                    if (it.id in bulkSelection) it.copy(gainDb = value) else it
                                                },
                                            )
                                        }
                                    },
                                    onValueChangeFinished = { commit(profile) },
                                    valueRange = -12f..12f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                )
                                Text("${bulkGain.roundToInt()} dB", style = MaterialTheme.typography.labelMedium)
                            }
                            Row {
                                TextButton(onClick = { commitBulk { it.copy(gainDb = 0f) } }) { Text("选中归零") }
                                TextButton(onClick = { autoPreamp() }) { Text("自动前级") }
                            }
                        }
                    }
                }

                if (profile.filters.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = meiPageGutter()),
                    ) {
                        items(profile.filters.size, key = { profile.filters[it].id }) { index ->
                            val filter = profile.filters[index]
                            FilterCard(
                                filter = filter,
                                number = index + 1,
                                selected = if (bulkMode) filter.id in bulkSelection else filter.id == selectedId,
                                onSelect = {
                                    if (bulkMode) {
                                        bulkSelection = if (filter.id in bulkSelection) {
                                            bulkSelection - filter.id
                                        } else bulkSelection + filter.id
                                    } else {
                                        selectedId = filter.id
                                    }
                                },
                                onEnabledChange = { enabled ->
                                    changeFilter(
                                        filter.id,
                                        { it.copy(enabled = enabled) },
                                        true
                                    )
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (selected != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = meiPageGutter()),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("滤波器 ${profile.filters.indexOfFirst { it.id == selected.id } + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f))
                                IconButton(onClick = { deleteFilterId = selected.id }) {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = "移除滤波器"
                                    )
                                }
                            }
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterType.entries.forEach { type ->
                                    FilterChip(
                                        selected = selected.type == type,
                                        onClick = {
                                            changeFilter(
                                                selected.id,
                                                { it.copy(type = type) },
                                                true
                                            )
                                        },
                                        label = { Text(type.label) },
                                    )
                                }
                            }
                            ParameterSlider(
                                "频率", selected.frequencyHz, 20f..20_000f, "Hz", 0,
                                logScale = true
                            ) { value, done ->
                                changeFilter(selected.id, { it.copy(frequencyHz = value) }, done)
                            }
                            if (selected.type.usesGain) {
                                ParameterSlider(
                                    "增益",
                                    selected.gainDb,
                                    -24f..24f,
                                    "dB",
                                    1
                                ) { value, done ->
                                    changeFilter(selected.id, { it.copy(gainDb = value) }, done)
                                }
                            }
                            ParameterSlider("Q 值", selected.q, 0.1f..12f, "Q", 2) { value, done ->
                                changeFilter(selected.id, { it.copy(q = value) }, done)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                PresetList(
                    presets = EqualizerPresets.builtIn + customPresets,
                    selectedId = profile.presetId,
                    onSelect = { preset ->
                        commit(preset.profile.copy(enabled = true, presetId = preset.id))
                        selectedId = preset.profile.filters.firstOrNull()?.id ?: -1
                        tab = 0
                    },
                    onDelete = { deletePresetId = it },
                )
            }
        }
    }

    if (showBundleDialog) {
        AlertDialog(
            onDismissRequest = { showBundleDialog = false },
            title = { Text("整套调音") },
            text = { Text("导出会把当前 EQ 和混响存成一个 JSON 文件；导入会同时应用两者。") },
            confirmButton = {
                TextButton(onClick = {
                    showBundleDialog = false
                    bundleExportPicker.launch("mei-tuning.json")
                }) { Text("导出") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBundleDialog = false
                    bundleImportPicker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }) { Text("导入") }
            },
        )
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存当前调音") },
            text = {
                OutlinedTextField(
                    value = name, onValueChange = { name = it.take(40) },
                    label = { Text("预设名称") }, singleLine = true
                )
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    val id = "user_${System.currentTimeMillis()}"
                    val preset = EqualizerPreset(id, name.trim(), profile.copy(presetId = id))
                    customPresets = customPresets + preset
                    scope.launch {
                        context.dataStore.edit {
                            it[UserEqualizerPresetsKey] =
                                EqualizerPresets.encodeCustom(customPresets)
                        }
                    }
                    commit(profile.copy(presetId = id))
                    showSaveDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("取消") } },
        )
    }
    deleteFilterId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteFilterId = null },
            title = { Text("移除滤波器？") },
            text = { Text("此滤波器的参数将被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    commit(
                        profile.copy(
                            presetId = "custom",
                            filters = profile.filters.filterNot { it.id == id })
                    )
                    selectedId = profile.filters.firstOrNull { it.id != id }?.id ?: -1
                    deleteFilterId = null
                }) { Text("移除") }
            },
            dismissButton = { TextButton(onClick = { deleteFilterId = null }) { Text("取消") } },
        )
    }
    deletePresetId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletePresetId = null },
            title = { Text("删除我的预设？") },
            confirmButton = {
                TextButton(onClick = {
                    customPresets = customPresets.filterNot { it.id == id }
                    scope.launch {
                        context.dataStore.edit {
                            it[UserEqualizerPresetsKey] =
                                EqualizerPresets.encodeCustom(customPresets)
                        }
                    }
                    if (profile.presetId == id) commit(profile.copy(presetId = "custom"))
                    deletePresetId = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletePresetId = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun FilterCard(
    filter: EqFilter,
    number: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onEnabledChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$number", style = MaterialTheme.typography.titleLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = filter.enabled, onCheckedChange = onEnabledChange,
                    modifier = Modifier.semantics { contentDescription = "滤波器 $number 开关" })
            }
            Spacer(Modifier.height(2.dp))
            Text(filter.type.label, style = MaterialTheme.typography.titleMedium)
            Text(
                formatHz(filter.frequencyHz), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PresetList(
    presets: List<EqualizerPreset>, selectedId: String,
    onSelect: (EqualizerPreset) -> Unit, onDelete: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = meiPageGutter()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.forEach { preset ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(preset) },
                colors = CardDefaults.cardColors(
                    containerColor = if (preset.id == selectedId)
                        MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow
                ),
            ) {
                Row(
                    Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            preset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${preset.profile.filters.size} 个滤波器  ·  ${formatDb(preset.profile.inputGainDb)} 输入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (preset.id.startsWith("user_")) {
                        IconButton(onClick = { onDelete(preset.id) }) {
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                contentDescription = "删除 ${preset.name}"
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    decimals: Int,
    logScale: Boolean = false,
    onChange: (Float, Boolean) -> Unit,
) {
    var showExactInput by remember { mutableStateOf(false) }
    var lastDraggedValue by remember { mutableFloatStateOf(value) }
    val display = if (unit == "Hz") formatHz(value) else if (unit == "Q")
        "Q ${formatNumber(value, decimals)}" else "${formatDb(value)}"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = { showExactInput = true }) { Text(display, maxLines = 1) }
    }
    val position = if (logScale) {
        ((log10(value.coerceAtLeast(20f)) - log10(range.start)) /
                (log10(range.endInclusive) - log10(range.start))).coerceIn(0f, 1f)
    } else value.coerceIn(range.start, range.endInclusive)
    Slider(
        value = position,
        onValueChange = { raw ->
            val actual = if (logScale) 10f.pow(
                log10(range.start) +
                        raw * (log10(range.endInclusive) - log10(range.start))
            ) else raw
            lastDraggedValue = if (logScale) actual.roundToInt().toFloat() else actual
            onChange(lastDraggedValue, false)
        },
        onValueChangeFinished = { onChange(lastDraggedValue, true) },
        valueRange = if (logScale) 0f..1f else range,
        modifier = Modifier.semantics { contentDescription = "$label $display" },
    )
    if (showExactInput) {
        var text by remember { mutableStateOf(formatNumber(value, decimals)) }
        val parsed = text.toFloatOrNull()
        AlertDialog(
            onDismissRequest = { showExactInput = false },
            title = { Text("设置$label") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("$label（$unit）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = {
                        Text(
                            if (unit == "Q") "0.1–12 · Q 越高，影响的频率范围越窄"
                            else "${
                                formatNumber(
                                    range.start,
                                    decimals
                                )
                            } – ${formatNumber(range.endInclusive, decimals)} $unit"
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = parsed != null && parsed.isFinite() && parsed in range,
                    onClick = { parsed?.let { onChange(it, true) }; showExactInput = false },
                ) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showExactInput = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun EqualizerGraph(
    profile: EqualizerProfile, response: DoubleArray, selectedId: Int,
    onSelect: (Int) -> Unit,
    onFilterDrag: (Int, Float, Float, Boolean) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val frequencies =
        listOf(31.5, 63.0, 125.0, 250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0, 16_000.0)
    val gains = listOf(24, 12, 0, -12, -24)
    val left = 42.dp
    val right = 12.dp
    val top = 14.dp
    val bottom = 30.dp
    var dragId by remember { mutableIntStateOf(-1) }
    var draggedFrequency by remember { mutableStateOf(1_000f) }
    var draggedGain by remember { mutableStateOf(0f) }
    val currentProfile by rememberUpdatedState(profile)
    val currentSelectedId by rememberUpdatedState(selectedId)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnFilterDrag by rememberUpdatedState(onFilterDrag)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(236.dp)
            .semantics {
                contentDescription = "频响曲线，先选择节点再拖动，或使用下方数值按钮精确调整"
            }
            .pointerInput(Unit) {
                val l = left.toPx();
                val r = right.toPx();
                val t = top.toPx();
                val b = bottom.toPx()
                val plotW = (size.width - l - r).coerceAtLeast(1f)
                val plotH = (size.height - t - b).coerceAtLeast(1f)
                detectTapGestures { offset ->
                    val nearest = currentProfile.filters.minByOrNull { filter ->
                        val px = l + (log10(
                            filter.frequencyHz.coerceIn(
                                20f,
                                20_000f
                            )
                        ) - log10(20f)) / 3f * plotW
                        val py =
                            t + (24f - if (filter.type.usesGain) filter.gainDb else 0f) / 48f * plotH
                        (px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y)
                    }
                    if (nearest != null) {
                        val px = l + (log10(
                            nearest.frequencyHz.coerceIn(
                                20f,
                                20_000f
                            )
                        ) - log10(20f)) / 3f * plotW
                        val py =
                            t + (24f - if (nearest.type.usesGain) nearest.gainDb else 0f) / 48f * plotH
                        if ((px - offset.x) * (px - offset.x) + (py - offset.y) * (py - offset.y) <=
                            34.dp.toPx() * 34.dp.toPx()
                        ) currentOnSelect(nearest.id)
                    }
                }
            }
            .pointerInput(Unit) {
                val l = left.toPx();
                val r = right.toPx();
                val t = top.toPx();
                val b = bottom.toPx()
                val plotW = (size.width - l - r).coerceAtLeast(1f)
                val plotH = (size.height - t - b).coerceAtLeast(1f)
                fun x(f: Float) = l + ((log10(f.coerceIn(20f, 20_000f)) - log10(20f)) / 3f) * plotW
                fun y(g: Float) = t + (24f - g.coerceIn(-24f, 24f)) / 48f * plotH
                detectDragGestures(
                    onDragStart = { offset ->
                        val nearest = currentProfile.filters.minByOrNull { filter ->
                            val dx = x(filter.frequencyHz) - offset.x
                            val dy = y(if (filter.type.usesGain) filter.gainDb else 0f) - offset.y
                            dx * dx + dy * dy
                        }
                        if (nearest != null) {
                            val dx = x(nearest.frequencyHz) - offset.x
                            val dy = y(if (nearest.type.usesGain) nearest.gainDb else 0f) - offset.y
                            if (nearest.id == currentSelectedId && dx * dx + dy * dy < (20.dp.toPx() * 20.dp.toPx())) {
                                dragId = nearest.id
                                draggedFrequency = nearest.frequencyHz
                                draggedGain = nearest.gainDb
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        if (dragId >= 0) {
                            draggedFrequency = 10f.pow(
                                1.30103f +
                                        ((change.position.x - l) / plotW).coerceIn(0f, 1f) * 3f
                            ).coerceIn(20f, 20_000f)
                            draggedGain =
                                (24f - ((change.position.y - t) / plotH) * 48f).coerceIn(-24f, 24f)
                            currentOnFilterDrag(dragId, draggedFrequency, draggedGain, false)
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (dragId >= 0) currentOnFilterDrag(
                            dragId,
                            draggedFrequency,
                            draggedGain,
                            true
                        )
                        dragId = -1
                    },
                    onDragCancel = {
                        if (dragId >= 0) currentOnFilterDrag(
                            dragId,
                            draggedFrequency,
                            draggedGain,
                            true
                        )
                        dragId = -1
                    },
                )
            },
    ) {
        val l = left.toPx();
        val r = right.toPx();
        val t = top.toPx();
        val b = bottom.toPx()
        val plotW = size.width - l - r
        val plotH = size.height - t - b
        fun x(f: Double) = l + ((log10(f) - log10(20.0)) / 3.0 * plotW).toFloat()
        fun y(g: Double) = t + ((24.0 - g.coerceIn(-24.0, 24.0)) / 48.0 * plotH).toFloat()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor.toArgb(); textSize = 11.dp.toPx(); textAlign = Paint.Align.CENTER
        }
        frequencies.forEach { f ->
            val px = x(f)
            drawLine(grid, Offset(px, t), Offset(px, t + plotH), 1.dp.toPx())
            drawContext.canvas.nativeCanvas.drawText(
                if (f >= 1_000) "${(f / 1_000).toInt()}k" else f.toInt().toString(),
                px, size.height - 7.dp.toPx(), paint
            )
        }
        gains.forEach { gain ->
            val py = y(gain.toDouble())
            drawLine(
                grid,
                Offset(l, py),
                Offset(l + plotW, py),
                if (gain == 0) 1.5.dp.toPx() else 1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                if (gain > 0) "+$gain" else "$gain",
                l / 2,
                py + 4.dp.toPx(),
                paint
            )
        }
        val curve = Path()
        response.forEachIndexed { index, gain ->
            val frequency =
                20.0 * 1_000.0.pow(index.toDouble() / (response.size - 1).coerceAtLeast(1))
            val point = Offset(x(frequency), y(gain))
            if (index == 0) curve.moveTo(point.x, point.y) else curve.lineTo(point.x, point.y)
        }
        drawPath(
            curve, if (profile.enabled) accent else textColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
        profile.filters.forEachIndexed { index, filter ->
            val center = Offset(
                x(filter.frequencyHz.toDouble()),
                y(if (filter.type.usesGain) filter.gainDb.toDouble() else 0.0)
            )
            val nodeColor = if (filter.enabled && profile.enabled) accent else textColor
            if (filter.id == selectedId) drawCircle(
                nodeColor.copy(alpha = 0.18f),
                21.dp.toPx(),
                center
            )
            drawCircle(surface, 12.dp.toPx(), center)
            drawCircle(nodeColor, 12.dp.toPx(), center, style = Stroke(2.dp.toPx()))
            drawContext.canvas.nativeCanvas.drawText(
                (index + 1).toString(), center.x, center.y + 4.dp.toPx(),
                Paint(paint).apply {
                    color = nodeColor.toArgb(); textSize = 12.dp.toPx(); isFakeBoldText = true
                })
        }
    }
}

private fun formatNumber(value: Float, digits: Int) =
    String.format(Locale.US, "%.${digits}f", value)

private fun formatDb(value: Float) = String.format(Locale.US, "%+.1f dB", value)
private fun formatHz(value: Float) = if (value < 1_000f) "${value.roundToInt()} Hz"
else String.format(Locale.US, "%.1f kHz", value / 1_000f)

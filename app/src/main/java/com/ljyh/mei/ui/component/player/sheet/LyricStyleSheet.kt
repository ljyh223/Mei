package com.ljyh.mei.ui.component.player.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import com.ljyh.mei.constants.AccompanimentLyricTextBoldKey
import com.ljyh.mei.constants.AccompanimentLyricTextSizeKey
import com.ljyh.mei.constants.LyricTextSize
import com.ljyh.mei.constants.NormalLyricTextBoldKey
import com.ljyh.mei.constants.NormalLyricTextSizeKey
import com.ljyh.mei.constants.PhoneticLyricTextBoldKey
import com.ljyh.mei.constants.PhoneticLyricTextSizeKey
import com.ljyh.mei.constants.ShowLyricPhoneticKey
import com.ljyh.mei.constants.ShowLyricTransitionKey
import com.ljyh.mei.ui.component.sheet.meiSheetContentHeight
import com.ljyh.mei.utils.preferences.rememberEnumPreference
import com.ljyh.mei.utils.preferences.rememberPreference
import kotlin.math.roundToInt

private enum class LyricStylePart(
    val label: String,
    val sizeKey: Preferences.Key<String>,
    val boldKey: Preferences.Key<Boolean>,
    val defaultSize: LyricTextSize,
    val visibilityKey: Preferences.Key<Boolean>? = null,
) {
    Main("主歌词", NormalLyricTextSizeKey, NormalLyricTextBoldKey, LyricTextSize.Size34),
    Translation("翻译", AccompanimentLyricTextSizeKey, AccompanimentLyricTextBoldKey,
        LyricTextSize.Size20, ShowLyricTransitionKey),
    Phonetic("音译", PhoneticLyricTextSizeKey, PhoneticLyricTextBoldKey,
        LyricTextSize.Size20, ShowLyricPhoneticKey),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricStyleSheet() {
    var part by rememberSaveable { mutableStateOf(LyricStylePart.Main) }
    Column(
        Modifier.fillMaxWidth().heightIn(max = meiSheetContentHeight(0.45f, 256.dp))
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            LyricStylePart.entries.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = part == value,
                    onClick = { part = value },
                    shape = SegmentedButtonDefaults.itemShape(index, LyricStylePart.entries.size),
                    label = { Text(value.label) },
                )
            }
        }
        key(part) {
            val (size, onSizeChange) = rememberEnumPreference(part.sizeKey, part.defaultSize)
            val (bold, onBoldChange) = rememberPreference(part.boldKey, true)
            val visibility = part.visibilityKey?.let { rememberPreference(it, true) }
            val sizes = LyricTextSize.entries
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("字号 ${size.text}", style = MaterialTheme.typography.labelLarge)
                IconButton(
                    onClick = { onSizeChange(sizes[size.ordinal - 1]) },
                    enabled = size.ordinal > 0,
                ) { Icon(Icons.Rounded.Remove, "减小字号") }
                Slider(
                    value = size.ordinal.toFloat(),
                    onValueChange = { index -> onSizeChange(sizes[index.roundToInt().coerceIn(sizes.indices)]) },
                    valueRange = 0f..sizes.lastIndex.toFloat(),
                    steps = sizes.size - 2,
                    modifier = Modifier.weight(1f).semantics { contentDescription = "歌词字号" },
                )
                IconButton(
                    onClick = { onSizeChange(sizes[size.ordinal + 1]) },
                    enabled = size.ordinal < sizes.lastIndex,
                ) { Icon(Icons.Rounded.Add, "增大字号") }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = bold,
                    onClick = { onBoldChange(!bold) },
                    label = { Text("加粗") },
                    leadingIcon = { Icon(Icons.Rounded.FormatBold, null) },
                )
                if (visibility != null) FilterChip(
                    selected = visibility.value,
                    onClick = { visibility.value = !visibility.value },
                    label = { Text("显示${part.label}") },
                )
            }
        }
    }
}

package com.ljyh.mei.ui.tablet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.glass.MeiGlassSurface
import com.ljyh.mei.ui.component.glass.meiChromeSelectionColor
import com.ljyh.mei.ui.component.glass.meiGlassAccentColor
import com.ljyh.mei.ui.component.glass.meiGlassContentColor
import com.ljyh.mei.ui.screen.Index

@Composable
internal fun TabletNavigationTabs(
    selectedRoute: String,
    showLabels: Boolean,
    enabled: Boolean,
    onSelect: (Index) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selection = meiChromeSelectionColor()
    val ink = meiGlassContentColor()
    val secondary = meiGlassContentColor(secondary = true)
    val accent = meiGlassAccentColor()
    MeiGlassSurface(modifier, shape = RoundedCornerShape(24.dp), shadowElevation = 0.dp) {
        Row(Modifier.fillMaxSize().padding(6.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Index.entries.forEach { tab ->
                val selected = tab.route == selectedRoute
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (selected) selection else Color.Transparent)
                        .selectable(selected, enabled = enabled, role = Role.Tab, onClick = { onSelect(tab) })
                        .semantics { contentDescription = tab.label },
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, null, Modifier.size(24.dp), tint = if (selected) accent else secondary)
                    if (showLabels) Text(
                        tab.label,
                        color = if (selected) ink else secondary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
            }
        }
    }
}

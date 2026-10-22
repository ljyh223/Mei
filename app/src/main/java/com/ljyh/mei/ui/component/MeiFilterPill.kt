package com.ljyh.mei.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MeiFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.RadioButton,
    compact: Boolean = false,
) {
    val fill by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "filterFill",
    )
    val ink = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    if (compact) {
        val interaction = remember { MutableInteractionSource() }
        // Keep the touch area separate from the smaller visible pill.
        Box(
            modifier = modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .selectable(selected = selected, role = role, onClick = onClick,
                    interactionSource = interaction, indication = null),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.clip(CircleShape).background(fill).indication(interaction, ripple())
                    .heightIn(min = 32.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, lineHeight = 18.sp), color = ink)
            }
        }
    } else {
        Box(
            modifier = modifier.clip(CircleShape).background(fill)
                .selectable(selected = selected, role = role, onClick = onClick)
                .heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) { Text(label, style = MaterialTheme.typography.labelLarge, color = ink) }
    }
}

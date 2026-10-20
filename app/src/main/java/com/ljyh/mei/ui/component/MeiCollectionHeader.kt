package com.ljyh.mei.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Artwork keeps its square shape while title and metadata measure independently below it. */
@Composable
fun MeiCollectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.Transparent,
    titleModifier: Modifier = Modifier,
    cover: @Composable BoxScope.() -> Unit,
    metadata: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(tint, MaterialTheme.colorScheme.background)),
        ),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(Modifier.widthIn(max = 960.dp).fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp)) {
            val wide = maxWidth >= 560.dp
            val artworkSize = if (wide) 200.dp else (maxWidth * 0.62f).coerceIn(160.dp, 248.dp)
            val artwork: @Composable () -> Unit = {
                Box(
                    modifier = Modifier.size(artworkSize).shadow(8.dp, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    content = cover,
                )
            }
            val identity: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, modifier = titleModifier, style = MaterialTheme.typography.headlineMedium,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                    metadata()
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    artwork()
                    Box(Modifier.weight(1f)) { identity() }
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(Modifier.align(Alignment.CenterHorizontally)) { artwork() }
                    identity()
                }
            }
        }
    }
}

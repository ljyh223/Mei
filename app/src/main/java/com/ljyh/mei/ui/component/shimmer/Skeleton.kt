package com.ljyh.mei.ui.component.shimmer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/**
 * Replaces a composable's drawing with a skeleton while preserving its measured size.
 *
 * Layout modifiers should be applied before this modifier. The same composable therefore owns
 * both its loading and loaded geometry, avoiding a separately maintained skeleton layout.
 */
@Composable
fun Modifier.skeleton(
    visible: Boolean,
    shape: Shape = RoundedCornerShape(6.dp),
): Modifier {
    if (!visible) return this

    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    return clip(shape).drawWithCache {
        onDrawWithContent {
            drawRect(color = color)
        }
    }.clearAndSetSemantics { }
}

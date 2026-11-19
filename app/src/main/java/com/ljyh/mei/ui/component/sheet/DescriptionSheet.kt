package com.ljyh.mei.ui.component.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DescriptionSheet(title: String, description: String, onDismiss: () -> Unit) {
    MeiAdaptiveSheet(onDismissRequest = onDismiss, compactTitle = title) {
        val compact = isCompactSideSheet()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = meiSheetContentHeight(.7f))
                .verticalScroll(rememberScrollState())
                .padding(
                    start = if (compact) 16.dp else 24.dp,
                    end = if (compact) 16.dp else 24.dp,
                    top = if (compact) 4.dp else 0.dp,
                    bottom = if (compact) 12.dp else 32.dp,
                ),
        ) {
            if (!compact) Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            )
            Text(
                text = description.trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

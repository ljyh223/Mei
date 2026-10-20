package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ljyh.mei.ui.component.MeiFilterPill

@Composable
fun ModernCapsuleTabs(tabs: List<String>, selectedIndex: Int, onTabClick: (Int) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(tabs.size) { index ->
            MeiFilterPill(tabs[index], selectedIndex == index, { onTabClick(index) }, role = Role.Tab)
        }
    }
}

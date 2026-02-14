package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun ModernCapsuleTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabClick: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp), // 容器内部的内边距
        horizontalArrangement = Arrangement.SpaceEvenly,   // 均匀分布，或者用 spacedBy
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs.size) { index ->
            val isSelected = selectedIndex == index

            // 选中态：使用主色，实心
            // 未选中态：透明背景，只显示文字
            val containerColor =
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
            val contentColor =
                if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            // 增加点击区域的 Surface
            Surface(
                onClick = { onTabClick(index) },
                shape = CircleShape,
                color = containerColor,
                // 稍微调整高度，让它比外面的“灵动岛”小一圈
                modifier = Modifier.height(40.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabs[index],
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = fontWeight),
                        color = contentColor
                    )
                }
            }
        }
    }
}

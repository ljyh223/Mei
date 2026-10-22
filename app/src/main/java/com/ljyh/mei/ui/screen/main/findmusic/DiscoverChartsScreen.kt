package com.ljyh.mei.ui.screen.main.findmusic

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.collection.MeiCollectionPage
import com.ljyh.mei.ui.local.LocalNavController
import com.ljyh.mei.ui.screen.Screen

@Composable
fun DiscoverChartsScreen(viewModel: DiscoverChartsViewModel = hiltViewModel()) {
    val nav = LocalNavController.current
    val result by viewModel.charts.collectAsState()
    val charts = (result as? Resource.Success)?.data.orEmpty()
    MeiCollectionPage(
        title = "排行榜", itemCount = charts.size, itemKey = { charts[it].id },
        onNavigateBack = { nav.navigateUp() }, onRetry = viewModel::reload,
        loading = result is Resource.Loading, failed = result is Resource.Error,
    ) { index ->
        val chart = charts[index]
        DiscoverChartRow(chart) { Screen.PlayList.navigate(nav) { addPath(chart.id.toString()) } }
    }
}

package com.ljyh.mei.ui.screen.main.library.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ljyh.mei.data.model.AlbumPhoto
import com.ljyh.mei.data.network.Resource
import com.ljyh.mei.ui.component.SingleImagePickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerSheet(
    photoAlbum: Resource<AlbumPhoto>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        when (photoAlbum) {
            is Resource.Success -> {
                SingleImagePickerSheet(
                    images = photoAlbum.data.data.records.map { it.imageUrl },
                    onSelect = onSelect,
                    onDismiss = onDismiss
                )
            }

            else -> {
                Box(Modifier
                    .fillMaxWidth()
                    .height(200.dp), contentAlignment = Alignment.Center) {
                    if (photoAlbum is Resource.Loading) CircularProgressIndicator()
                    else Text("无法加载图片")
                }
            }
        }
    }
}
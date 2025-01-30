package com.ljyh.music.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.window.DialogProperties


@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    openDialog: MutableState<Boolean>
) {
    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
                onDismiss()
            },
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                    onConfirm()
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    openDialog.value = false
                    onDismiss()
                }) {
                    Text("取消")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}
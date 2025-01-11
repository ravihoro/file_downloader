package com.example.filedownloader

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun UrlInputDialog(
    fileName: String,
    urlInput: String,
    onFileNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddDownload: () -> Unit
) {
    Log.d("DownloadManager", "Alert Dialog")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter details to start download") },
        text = {
            Column {

                TextField(
                    value = fileName,
                    onValueChange = onFileNameChange,
                    label = { Text("File Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )

                TextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    label = { Text("Download URL") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Uri,
                    )
                )
            }
        },
        confirmButton = {
            Button(onClick = onAddDownload, enabled = urlInput.isNotEmpty() && fileName.isNotEmpty()) {
                Text("Start Download")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
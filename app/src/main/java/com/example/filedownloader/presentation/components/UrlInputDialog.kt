package com.example.filedownloader.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlInputDialog(
    urlInput: String,
    isLoading: Boolean,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddDownload: () -> Unit,
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter URL to start download") },
        text = {
            TextField(
                value = urlInput,
                onValueChange = onUrlChange,
                label = { Text("Enter URL")},
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            if(isLoading){
                CircularProgressIndicator()
            }else{
                Button(onClick = onAddDownload, enabled = urlInput.isNotEmpty()) {
                    Text("Start Download")
                }
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
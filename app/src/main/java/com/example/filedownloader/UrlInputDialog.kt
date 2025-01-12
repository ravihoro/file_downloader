package com.example.filedownloader

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.flow.StateFlow

@Composable
fun UrlInputDialog(
    urlInput: String,
    isLoadingFlow: StateFlow<Boolean>,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddDownload: () -> Unit
) {

    val isLoading = isLoadingFlow.collectAsState();

    Log.d("DownloadManager", "Alert Dialog")
    AlertDialog(

        onDismissRequest = onDismiss,
        title = { Text("Enter details to start download") },
        text = {
            Column {
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
            if(isLoading.value){
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
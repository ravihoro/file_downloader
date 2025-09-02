package com.example.filedownloader.presentation.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.filedownloader.presentation.event.UrlInputDialogEvent
import com.example.filedownloader.presentation.viewmodel.UrlInputDialogViewModel

@Composable
fun UrlInputDialog(
    urlInputDialogViewModel: UrlInputDialogViewModel = hiltViewModel()
) {

    val urlInputDialogState by urlInputDialogViewModel.urlInputDialogState.collectAsStateWithLifecycle()

    val onDismiss = {
        urlInputDialogViewModel.onEvent(UrlInputDialogEvent.HideDialog)
    }

    val context = LocalContext.current

    if(urlInputDialogState.showDialog){
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Enter URL to start download") },
            text = {
                Column {
                    TextField(
                        value = urlInputDialogState.urlInput,
                        onValueChange = {
                            urlInputDialogViewModel.onEvent(
                                UrlInputDialogEvent.OnInputChange(
                                    it
                                )
                            )
                        },
                        label = { Text("Enter URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(5.dp))
                    if (!urlInputDialogState.error.isNullOrEmpty()) {
                        Text(urlInputDialogState.error.toString(), color = Color.Red, fontSize = 12.sp)
                    }

                }
            },
            confirmButton = {
                if (urlInputDialogState.isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        if(urlInputDialogState.urlInput.trim().isNotBlank()){
                            urlInputDialogViewModel.onEvent(
                                UrlInputDialogEvent.FetchMetaData(urlInputDialogState.urlInput.trim())
                            )
                        }else{
                            Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                        }
                    }, enabled = urlInputDialogState.urlInput.isNotEmpty()) {
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
}
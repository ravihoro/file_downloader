package com.example.filedownloader

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ActiveDownloadList(downloadManager: DownloadManager) {
    val activeDownloads by downloadManager.activeDownloads.collectAsState()
    val isLoading by downloadManager.isLoading.collectAsState();

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeDownloads.isEmpty()) {
            Text("No active downloads", modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(activeDownloads.entries.toList()) { (taskId, task) ->
                    DownloadItem(task = task, downloadManager = downloadManager)
                    HorizontalDivider(thickness = 1.dp, color = Color.Gray)
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red
            )
        }
    }
}
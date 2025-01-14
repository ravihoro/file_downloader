package com.example.filedownloader

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


@Composable
fun DownloadItem(task: DownloadTask, downloadManager: DownloadManager){
    task.let {
        val progress = it.progress
        val status = it.status

        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text("File: ${it.fileName}")
                Text("Status: $status")

                Text("Progress: $progress %")

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Blue,
                    trackColor = Color.Gray,
                )

                when(status){
                    DownloadStatus.ACTIVE -> {
                        Row {
                            Button(onClick = {downloadManager.pauseDownload(it.id)}) {
                                Text("Pause")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {downloadManager.cancelDownload(it.id)}){
                                Text("Cancel")
                            }
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        Row{
                            Button(onClick = {downloadManager.resumeDownload(it)}) {
                                Text("Resume")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {downloadManager.cancelDownload(it.id)}){
                                Text("Cancel")
                            }
                        }
                    }
                    DownloadStatus.COMPLETED -> Text("Download Complete")
                    DownloadStatus.CANCELLED -> Text("Download Cancelled")
                }

            }

        }
    }
}
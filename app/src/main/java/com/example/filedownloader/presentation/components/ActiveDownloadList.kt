package com.example.filedownloader.presentation.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.filedownloader.data.local.DownloadTask

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun ActiveDownloadList(
    downloads: List<DownloadTask>,
    onPause: (DownloadTask) -> Unit,
    onResume: (DownloadTask) -> Unit,
    onCancel: (DownloadTask) -> Unit,
    ) {

    if(downloads.isEmpty()){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text("No Active Downloads")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(downloads){ task ->
                DownloadItem(
                    task = task,
                    onPause = {onPause(task)},
                    onCancel = {onCancel(task)},
                    onResume = {onResume(task)}
                )

                HorizontalDivider()
            }
        }
    }

}
package com.example.filedownloader

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadList(downloadManager: DownloadManager) {
    val activeDownloads by downloadManager.activeDownloads.collectAsState()
    var fileName by remember { mutableStateOf("")}
    var urlInput by remember { mutableStateOf("") }
    var showDialog by remember{ mutableStateOf(false)}
    val isLoading by downloadManager.isLoading.collectAsState();

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("File Downloader")})
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {showDialog = true}, modifier = Modifier.padding(16.dp)) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Download")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()){
            if (activeDownloads.isEmpty()) {
                Text("No active downloads", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(activeDownloads.entries.toList()) { (taskId, task) ->
                        DownloadItem(task = task, downloadManager = downloadManager)
                    }
                }
            }


            if(isLoading){
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red
                )
            }

        }
    }


    if(showDialog){
        UrlInputDialog(
            fileName = fileName,
            urlInput = urlInput,
            onFileNameChange = {fileName = it},
            onUrlChange = {urlInput = it},
            onDismiss = {showDialog = false},
            onAddDownload = {
                Log.d("DownloadManager", "Adding: $fileName $urlInput")
                downloadManager.startDownload(
                    DownloadTask(fileName = fileName, url = urlInput)
                )
                showDialog = false
            }
        )
    }

}
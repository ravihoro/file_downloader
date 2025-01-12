package com.example.filedownloader

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadPage(downloadManager: DownloadManager) {

    var showDialog by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var fileName by remember { mutableStateOf("") }
    var urlInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        downloadManager.getActiveDownloadsFromDb();
        downloadManager.getCompletedDownloadsFromDb();
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("File Downloader") }) }, floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add New Download")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedIndex) {
                Tab(
                    selectedIndex == 0,
                    onClick = { selectedIndex = 0 },
                    text = { Text("Active Downloads") }
                )

                Tab(
                    selectedIndex == 1,
                    onClick = { selectedIndex = 1 },
                    text = { Text("Completed Downloads") }
                )
            }

            when (selectedIndex) {
                0 -> ActiveDownloadList(downloadManager)
                1 -> CompletedDownloadList(downloadManager)
            }

        }
    }

    if (showDialog) {
        UrlInputDialog(
            urlInput = urlInput,
            isLoadingFlow = downloadManager.isLoading,
            onUrlChange = { urlInput = it },
            onDismiss = { showDialog = false },
            onAddDownload = {
                Log.d("DownloadManager", "Adding: $fileName $urlInput")

                downloadManager.getFileMetaData(urlInput);

                showDialog = false
            }
        )
    }


}
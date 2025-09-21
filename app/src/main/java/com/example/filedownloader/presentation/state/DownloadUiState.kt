package com.example.filedownloader.presentation.state

import com.example.filedownloader.data.local.DownloadTask

data class DownloadUiState(

    val active: List<DownloadTask> = emptyList(),
    val completed: List<DownloadTask> =  emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

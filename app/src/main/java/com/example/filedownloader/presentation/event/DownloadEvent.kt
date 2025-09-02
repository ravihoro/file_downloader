package com.example.filedownloader.presentation.event

import com.example.filedownloader.data.local.DownloadTask

sealed class DownloadEvent {

    data class Add(val url: String): DownloadEvent()
    data class Start(val task: DownloadTask): DownloadEvent()
    data class Pause(val taskId: Int, val downloadedBytes: Long, val progress: Float) : DownloadEvent()
    data class Resume(val task: DownloadTask): DownloadEvent()
    data class Cancel(val taskId: Int): DownloadEvent()

}
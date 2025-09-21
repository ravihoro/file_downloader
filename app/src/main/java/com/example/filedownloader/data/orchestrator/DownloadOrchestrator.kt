package com.example.filedownloader.data.orchestrator

interface DownloadOrchestrator {

    fun enqueueDownload(taskId: Int)
    fun pauseDownload(taskId: Int)
    fun cancelDownload(taskId: Int)
    fun resumeDownload(taskId: Int)
    suspend fun startNextDownload()
}
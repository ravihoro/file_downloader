package com.example.filedownloader.domain.usecase

import android.content.Context
import androidx.work.WorkManager
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
import com.example.filedownloader.data.repository.DownloadTaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PauseDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val orchestrator: DownloadOrchestrator
) {

    suspend operator fun invoke(taskId: Int, downloadedBytes: Long, progress: Float) {

        orchestrator.cancelDownload(taskId)

        repository.updateTaskProgress(taskId,progress, DownloadStatus.PAUSED, downloadedBytes, "0 B/s",)
    }

}
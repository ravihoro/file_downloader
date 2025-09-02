package com.example.filedownloader.domain.usecase

import android.content.Context
import androidx.work.WorkManager
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CancelDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val fileRepository: FileRepository,
    private val orchestrator: DownloadOrchestrator
) {

    suspend operator fun invoke(taskId: Int) {
        val task = repository.getTaskById(taskId)

        if(task != null){
            fileRepository.deleteFromCache(task.fileName)

            repository.updateTaskProgress(
                id = taskId,
                progress = 0f,
                status =  DownloadStatus.CANCELLED,
                downloadedBytes = 0L
            )

            orchestrator.cancelDownload(taskId)
        }
    }

}
package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.orchestrator.DownloadOrchestrator
import com.example.filedownloader.data.repository.DownloadTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val orchestrator: DownloadOrchestrator
){

    suspend operator fun invoke(task: DownloadTask): Long = withContext(Dispatchers.IO) {

        if(task.status == DownloadStatus.COMPLETED){
            return@withContext task.id.toLong()
        }

        val taskId = repository.insertOrUpdate(task.copy(status = DownloadStatus.ACTIVE))

        orchestrator.enqueueDownload(taskId.toInt())

        taskId

    }

}
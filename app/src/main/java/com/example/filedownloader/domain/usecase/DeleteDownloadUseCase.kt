package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import javax.inject.Inject

class DeleteDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val fileRepository: FileRepository,
) {

    suspend operator fun invoke(task: DownloadTask) {
        fileRepository.deleteFromDownloads(task.fileName);
        repository.deleteTask(task)
    }

}
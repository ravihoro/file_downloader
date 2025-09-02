package com.example.filedownloader.domain.usecase

import android.content.Context
import androidx.work.WorkManager
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.repository.DownloadTaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CancelDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    @ApplicationContext private val context: Context,
) {

    suspend operator fun invoke(taskId: Int) {
        repository.updateTaskProgress(
            id = taskId,
            progress = 0f,
            status =  DownloadStatus.CANCELLED,
            downloadedBytes = 0L
        )

        WorkManager.getInstance(context).cancelUniqueWork("download_$taskId")
    }

}
package com.example.filedownloader.domain.usecase

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StartDownloadUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    @ApplicationContext private val context: Context
){

    suspend operator fun invoke(task: DownloadTask): Long = withContext(Dispatchers.IO) {
        val taskId = repository.insertOrUpdate(task.copy(status = DownloadStatus.ACTIVE))

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf("TASK_ID" to taskId.toInt()))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        taskId

    }

}
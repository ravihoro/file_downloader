package com.example.filedownloader.data.orchestrator

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.filedownloader.data.worker.DownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WorkManagerDownloadOrchestrator @Inject constructor(

    @ApplicationContext private val context: Context

) : DownloadOrchestrator {
    private val workManager = WorkManager.getInstance(context)

    override fun enqueueDownload(taskId: Int) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setInputData(workDataOf("TASK_ID" to taskId.toInt()))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "download_$taskId",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelDownload(taskId: Int) {
        workManager.cancelUniqueWork("download_$taskId")
    }

    override fun resumeDownload(taskId: Int) {
        enqueueDownload(taskId)
    }
}
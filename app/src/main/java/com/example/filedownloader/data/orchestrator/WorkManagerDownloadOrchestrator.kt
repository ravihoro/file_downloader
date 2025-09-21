package com.example.filedownloader.data.orchestrator

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.repository.DownloadTaskRepository
//import com.example.filedownloader.data.worker.DownloadWorker
import com.example.filedownloader.di.IoScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

//class WorkManagerDownloadOrchestrator @Inject constructor(
//    @ApplicationContext private val context: Context,
//    @IoScope private val ioScope: CoroutineScope,
//    private val repository: DownloadTaskRepository,
//) : DownloadOrchestrator {
//    private val workManager = WorkManager.getInstance(context)
//    companion object {
//        private const val MAX_PARALLEL = 4
//    }
//
//    override fun enqueueDownload(taskId: Int) {
//        ioScope.launch {
//            val runningCount = workManager.getWorkInfosByTag("download")
//                .get()
//                .count { it.state == WorkInfo.State.RUNNING }
//
//            if(runningCount < MAX_PARALLEL){
//                startDownload(taskId)
//            }else{
//                val task = repository.getTaskById(taskId)
//                if(task != null){
//                    repository.insertOrUpdate(task.copy(status = DownloadStatus.QUEUED))
//                }
//            }
//        }
//    }
//
//    private fun startDownload(taskId: Int) {
//        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
//            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
//            .setInputData(workDataOf("TASK_ID" to taskId.toInt()))
//            .setBackoffCriteria(
//                BackoffPolicy.EXPONENTIAL,
//                10,
//                TimeUnit.SECONDS
//            )
//            .addTag("download")
//            .build()
//
//        workManager.enqueueUniqueWork(
//            "download_$taskId",
//            ExistingWorkPolicy.KEEP,
//            request
//        )
//    }
//
//    override fun cancelDownload(taskId: Int) {
//        workManager.cancelUniqueWork("download_$taskId")
//    }
//
//    override fun resumeDownload(taskId: Int) {
//        enqueueDownload(taskId)
//    }
//
//    override suspend fun startNextDownload(){
//        ioScope.launch {
//            val task = repository.getNextTaskByStatus(DownloadStatus.QUEUED)
//            if(task != null){
//                startDownload(task.id)
//            }
//        }
//    }
//}
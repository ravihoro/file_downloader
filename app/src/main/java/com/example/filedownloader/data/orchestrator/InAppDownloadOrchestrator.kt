package com.example.filedownloader.data.orchestrator

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.manager.InAppDownloadManager
import com.example.filedownloader.data.repository.DownloadTaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InAppDownloadOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: DownloadTaskRepository,
    private val downloadManager: InAppDownloadManager,
) : DownloadOrchestrator {

    companion object {
        private const val MAX_PARALLEL = 4
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun enqueueDownload(taskId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val running = downloadManager.activeCount()
            val task = repository.getTaskById(taskId) ?: return@launch

            if(running < MAX_PARALLEL){
                downloadManager.start(taskId)
            }else{
                repository.insertOrUpdate(task.copy(status = DownloadStatus.QUEUED))
            }
        }
    }

    override fun cancelDownload(taskId: Int) {
        downloadManager.cancel(taskId)
    }

    override fun pauseDownload(taskId: Int) {
        downloadManager.pause(taskId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun resumeDownload(taskId: Int) {
        downloadManager.resume(taskId)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun startNextDownload() {
        CoroutineScope(Dispatchers.IO).launch {
            val next = repository.getNextTaskByStatus(DownloadStatus.QUEUED)
            next?.let { enqueueDownload(it.id) }
        }
    }

}
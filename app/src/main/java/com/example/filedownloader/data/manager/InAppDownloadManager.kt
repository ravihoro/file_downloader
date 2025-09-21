package com.example.filedownloader.data.manager

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.notification.DownloadNotificationManager
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import com.example.filedownloader.data.repository.RemoteDownloadDataRepository
import com.example.filedownloader.data.service.DownloadForegroundService
import com.example.filedownloader.di.IoScope
import com.example.filedownloader.utils.formatSpeed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.FileOutputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ensureActive

@Singleton
class InAppDownloadManager @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val fileRepository: FileRepository,
    private val remoteRepo: RemoteDownloadDataRepository,
    private val notificationManager: DownloadNotificationManager,
    @ApplicationContext private val context: Context,
    @IoScope private val ioScope: CoroutineScope,
){

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Int, Job>()

    private val MAX_PARALLEL = 4

    private val semaphore = Semaphore(MAX_PARALLEL)

    private val lastDbWriteAt = ConcurrentHashMap<Int, Long>()
    private val lastProgressPct = ConcurrentHashMap<Int, Int>()

    fun activeCount(): Int = jobs.size

    @RequiresApi(Build.VERSION_CODES.Q)
    fun start(taskId: Int){
        if(jobs.containsKey(taskId)) return

        val acquired = semaphore.tryAcquire()
        if (!acquired) {
            ioScope.launch {
                // mark queued in DB and return — will be picked up when slots free
                val task = repository.getTaskById(taskId)
                if (task != null) {
                    repository.insertOrUpdate(task.copy(status = DownloadStatus.QUEUED))
                }
            }
            return
        }

        val initialNotif = notificationManager.createDownloadNotification(
            taskId = taskId,
            title = "", // generic title - we'll update it once we read DB
            progress = 0
        )

        DownloadForegroundService.NotificationStore.put(taskId, initialNotif)

        val job = scope.launch {
            try{
                downloadLoop(taskId)
            }finally {
                semaphore.release()
                jobs.remove(taskId)
                if(jobs.isEmpty()){
                    DownloadForegroundService.stopIfIdle(context)
                }
                tryStartNextQueued()
            }
        }

        val wasEmpty = jobs.isEmpty()
        jobs[taskId] = job

        DownloadForegroundService.updateServiceSummary(context)

        if (wasEmpty) {
            // we transitioned 0 -> 1, start the service now
            DownloadForegroundService.startServiceForTask(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun resume(taskId: Int){
        start(taskId)
    }

    fun pause(taskId: Int){
        jobs[taskId]?.cancel(CancellationException("Paused by User"))

        ioScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            val cacheFile = fileRepository.getCacheFile(task.fileName)
            val bytes = if(cacheFile.exists()) cacheFile.length() else 0L
            repository.updateTaskProgress(taskId, task.progress, DownloadStatus.PAUSED, bytes, "0 B/s")

            cancelNotification(taskId)
        }
    }

    fun cancel(taskId: Int){
        jobs[taskId]?.cancel(CancellationException("Cancelled by user"))
        ioScope.launch {
            val task = repository.getTaskById(taskId) ?: return@launch
            fileRepository.deleteFromCache(task.fileName)
            repository.updateTaskProgress(taskId, 0f, DownloadStatus.CANCELLED, 0L, "0 B/s")
            cancelNotification(taskId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun downloadLoop(taskId: Int) {
        val task = repository.getTaskById(taskId) ?: return

        var downloadedBytes: Long

        val cacheFile = fileRepository.getCacheFile(task.fileName).apply {
            parentFile?.mkdirs()
        }

        downloadedBytes = if(cacheFile.exists()) cacheFile.length() else 0L

        repository.updateTaskProgress(taskId, task.progress, DownloadStatus.ACTIVE, downloadedBytes, "0 B/s")

        val result = remoteRepo.startDownload(task.url, downloadedBytes, task.supportsResume)

        result.fold(onSuccess = { response ->
            val body = response.body ?: throw Exception("Empty response body")
            val totalBytes = if(task.totalBytes > 0) task.totalBytes else (body.contentLength().takeIf { it > 0 } ?: 0L )

            val sink = FileOutputStream(cacheFile, task.supportsResume).sink().buffer()

            val source = body.source()

            var lastDbTime = lastDbWriteAt[taskId] ?: 0L

            var lastDbBytes = downloadedBytes

            var lastPct = lastProgressPct[taskId] ?: task.progress.toInt()

            try {

                while(true){
                    currentCoroutineContext().ensureActive()

                    val read = source.read(sink.buffer, 8_192)
                    if(read == -1L) break

                    sink.emit()

                    downloadedBytes += read

                    val pct = if(totalBytes > 0) ((downloadedBytes * 100) / totalBytes).toInt() else 0

                    val now = System.currentTimeMillis()

                    val elapsedSinceWrite = now - lastDbTime

                    val shouldWrite = elapsedSinceWrite >= 1000L || pct >= (lastPct + 1)

                    if(shouldWrite){
                        val bytesInterval = downloadedBytes - lastDbBytes
                        val speed = if(bytesInterval > 0 && elapsedSinceWrite > 0) formatSpeed(bytesInterval * 1000.0 / (if (elapsedSinceWrite == 0L) 1L else elapsedSinceWrite)) else "0 B/s"
                        val progressFloat = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f

                        repository.updateTaskProgress(taskId, progressFloat, DownloadStatus.ACTIVE, downloadedBytes, speed)

                        val notifProgress = pct.coerceIn(0, 100)

                        val notif = notificationManager.createDownloadNotification(taskId, task.fileName, notifProgress)

                        DownloadForegroundService.updateNotification(context, taskId, notif)

                        lastDbTime = now
                        lastDbBytes = downloadedBytes
                        lastPct = pct
                        lastDbWriteAt[taskId] = lastDbTime
                        lastProgressPct[taskId] = lastPct
                    }
                }

                sink.flush()

                val saved = try {
                    fileRepository.saveFileToDownloads(context, cacheFile, task.mimeType)
                } catch (e: Exception){
                    Log.w("InAppDownloadManager", "Error saving to downloads: ${e.message}")
                    false
                }

                if(saved){
                    cacheFile.delete()
                    cancelNotification(taskId)

                    repository.updateTaskProgress(taskId, 100f, DownloadStatus.COMPLETED, downloadedBytes, "0 B/s")
                    notificationManager.showDownloadCompleteNotification(taskId + 10000, task.fileName)
                }else{
                    val p = ((downloadedBytes * 100f) / (if (totalBytes > 0) totalBytes else downloadedBytes)).coerceAtMost(99f)
                    repository.updateTaskProgress(taskId, p, DownloadStatus.PAUSED, downloadedBytes, "0 B/s")

                    cancelNotification(taskId)
                }

            }finally {
                try {
                    source.close()
                } catch (_: Throwable){}

                try {
                    sink.close()
                }catch (_: Throwable){}
            }

        }, onFailure = {
            repository.updateTaskProgress(taskId, task.progress, DownloadStatus.PAUSED, downloadedBytes, "0 B/s")
            cancelNotification(taskId)
        },)

    }

    private fun cancelNotification(taskId: Int) {
        notificationManager.cancelNotification(taskId)
        DownloadForegroundService.NotificationStore.remove(taskId)
        if (DownloadForegroundService.NotificationStore.mapSize() == 0) {
            // stop the service (ACTION_STOP) which calls stopForeground(true) and clears store
            DownloadForegroundService.stopIfIdle(context)
        } else {
            // otherwise just refresh summary
            DownloadForegroundService.updateServiceSummary(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun tryStartNextQueued() {
        ioScope.launch {
            try {
                val next = repository.getNextTaskByStatus(DownloadStatus.QUEUED)
                if (next != null) {
                    // simple capacity check to avoid creating jobs that will block on semaphore
                    if (jobs.size < MAX_PARALLEL) {
                        start(next.id)
                    } else {
                        // still no capacity — nothing to do; it will be tried again later
                    }
                }
            } catch (t: Throwable) {
                Log.w("InAppDownloadManager", "tryStartNextQueued failed: ${t.message}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun reconcileOnStartup(resumeActiveToAuto: Boolean = false){
        withContext(Dispatchers.IO) {
            val all = repository.getAllTasks().firstOrNull() ?: emptyList()
            all.forEach{t ->
                when (t.status) {
                    DownloadStatus.ACTIVE -> {
                        if (resumeActiveToAuto) resume(t.id) else repository.updateTaskProgress(t.id, t.progress, DownloadStatus.PAUSED, t.downloadedBytes, "0 B/s")
                    }
                    else -> {}
                }
            }
        }
    }

}
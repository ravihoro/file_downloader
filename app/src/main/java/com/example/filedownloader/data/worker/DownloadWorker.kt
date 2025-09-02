package com.example.filedownloader.data.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.filedownloader.data.notification.DownloadNotificationManager
import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.repository.DownloadTaskRepository
import com.example.filedownloader.data.repository.FileRepository
import com.example.filedownloader.data.repository.RemoteDownloadDataRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.buffer
import okio.sink
import java.io.FileOutputStream

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: DownloadTaskRepository,
    private val remoteDownloadDataRepository: RemoteDownloadDataRepository,
    private val fileRepository: FileRepository,
    private val notificationManager: DownloadNotificationManager
    ) : CoroutineWorker(context, params){

        @RequiresApi(Build.VERSION_CODES.Q)
        override suspend fun  doWork(): Result = withContext(Dispatchers.IO) {

            val taskId = inputData.getInt("TASK_ID", -1)

            if(taskId == -1) return@withContext Result.failure()

            val task = repository.getTaskById(taskId) ?:  return@withContext Result.failure()

            var downloadedBytes = 0L
            var lastProgress = 0

            try {

                Log.d("DownloadWorker", "Starting download for ${task.fileName}")

                setForeground(createForegroundInfo(taskId, task.fileName, 0))

                val file = fileRepository.getCacheFile(fileName = task.fileName)

                downloadedBytes = if(file.exists()) file.length() else 0L

                val responseResult = remoteDownloadDataRepository.startDownload(task.url, downloadedBytes, task.supportsResume)

                responseResult.fold(
                    onSuccess = { response ->
                        val totalBytes = if(task.totalBytes> 0) task.totalBytes else response.body?.contentLength() ?: 0L

                        val outputStream = FileOutputStream(file,task.supportsResume)

                        val sink = outputStream.sink().buffer()

                        val source = response.body?.source() ?: throw Exception("Empty Body")

                        while (true) {
                            if(isStopped){
                                Log.d("DownloadWorker", "Worker stopped, saving progress")
                                repository.updateTaskProgress(
                                    taskId,
                                    lastProgress.toFloat(),
                                    DownloadStatus.PAUSED,
                                    downloadedBytes
                                )
                                return@withContext Result.retry()
                            }

                            val bytesRead = source.read(sink.buffer,8_192)
                            if(bytesRead == -1L) break

                            sink.emit()

                            downloadedBytes += bytesRead

                            val progress = if(totalBytes > 0)
                                ((downloadedBytes.toFloat()/totalBytes) * 100).toInt()
                            else 0

                            if(progress > lastProgress){
                                lastProgress = progress
                                repository.updateTaskProgress(
                                    taskId,
                                    progress.toFloat(),
                                    DownloadStatus.ACTIVE,
                                    downloadedBytes
                                )
                                setForeground(createForegroundInfo(taskId,task.fileName, progress))
                            }

                        }

                        sink.flush()
                        sink.close()
                        source.close()
                        outputStream.close()

                        val isFileSavedToDownloads = fileRepository.saveFileToDownloads(context, file, task.mimeType)

                        if(isFileSavedToDownloads){
                            file.delete();

                            repository.updateTaskProgress(
                                taskId,
                                100f,
                                DownloadStatus.COMPLETED,
                                downloadedBytes
                            )

                            notificationManager.showDownloadCompleteNotification(taskId, task.fileName)

                            Log.d("DownloadWorker", "Download complete for ${task.fileName}")

                            Result.success()


                        }else{
                            repository.updateTaskProgress(
                                taskId,
                                (downloadedBytes * 100f / (task.totalBytes.takeIf { it > 0 } ?: downloadedBytes)).coerceAtMost(99f),
                                DownloadStatus.PAUSED,
                                downloadedBytes
                            )

                            // don’t delete the cache file, let user resume or retry
                            notificationManager.cancelNotification(taskId)

                            Result.retry()
                        }


                    },
                    onFailure = { e ->
                        throw Exception("HTTP error: ${e.message}")
                    }
                )
            }catch(e: Exception){
                Log.e("DownloadWorker", "Error: ${e.message}")
                repository.updateTaskProgress(
                    taskId,
                    lastProgress.toFloat(),
                    DownloadStatus.PAUSED,
                    downloadedBytes
                )
                notificationManager.cancelNotification(taskId)
                Result.retry()
            }

        }


    private fun createForegroundInfo(taskId: Int, fileName: String, progress: Int): ForegroundInfo {
        val notification = notificationManager.createDownloadNotification(
            taskId,
            fileName,
            progress,
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                taskId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(taskId, notification)
        }
    }

}
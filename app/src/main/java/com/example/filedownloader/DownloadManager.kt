package com.example.filedownloader

import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val repository: DownloadTaskRepository,
    @ApplicationContext private val context: Context,
) {
    @Inject
    lateinit var notificationManager: DownloadNotificationManager;

    private val client = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).followRedirects(true)
        .followSslRedirects(true).build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeDownloads = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap())
    val activeDownloads: StateFlow<Map<Int, DownloadTask>> get() = _activeDownloads

    private val _completedDownloads = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap());
    val completedDownloads: StateFlow<Map<Int, DownloadTask>> = _completedDownloads;

    private val activeDownloadJobs = mutableMapOf<Int, Job>()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading;

    private val maxParallelDownloads = 4;

    private val userAgent = "User-Agent";
    private val userAgentValue =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    fun getActiveDownloadsFromDb() {

        coroutineScope.launch {
            val activeList = repository.getTasksByStatus(DownloadStatus.ACTIVE)
            val pausedList = repository.getTasksByStatus(DownloadStatus.PAUSED)

            val activeDownloadsFromDb = activeList.associateBy { it.id }
            val pausedDownloadsFromDb = pausedList.associateBy { it.id }

            val combinedDownloads = activeDownloadsFromDb + pausedDownloadsFromDb

            _activeDownloads.emit(combinedDownloads)
        }
    }

    fun getCompletedDownloadsFromDb() {
        coroutineScope.launch {
            val completedList = repository.getTasksByStatus(DownloadStatus.COMPLETED)

            val completedDownloadsFromDb = completedList.associateBy { it.id }

            _completedDownloads.emit(completedDownloadsFromDb)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getFileMetaData(url: String) {
        coroutineScope.launch {
            try {
                _isLoading.value = true;

                val request = Request.Builder().url(url).header(
                    userAgent,
                    userAgentValue,
                ).addHeader("Range", "bytes=0-").head().build()

                val response = client.newCall(request).execute();

                if (response.isSuccessful) {
                    val headers = response.headers
                    val contentDisposition = headers["Content-Disposition"]
                    val contentLength = headers["Content-Length"]?.toLongOrNull()
                    val contentType =
                        headers["Content-Type"] ?: "application/octet-stream" // Fallback MIME type

                    val fileName =
                        if (contentDisposition != null && contentDisposition.contains("filename=")) {
                            contentDisposition.substringAfter("filename=").trim('"')
                        } else {
                            url.substringAfterLast("/")
                        }


                    Log.d("DownloadManager", "$contentLength $contentType")

                    val taskInDb = repository.getTaskByFileNameAndMimeType(fileName, contentType);

                    if (taskInDb != null) {
                        if (taskInDb.status == DownloadStatus.COMPLETED) throw Exception("File already downloaded")
                        startDownload(taskInDb);
                    } else {
                        var downloadTask = DownloadTask(
                            fileName = fileName,
                            url = url,
                            totalBytes = contentLength ?: 0L,
                            progress = 0f,
                            mimeType = contentType,
                            supportsResume = (response.code == 206 && response.header("Content-Range") != null)
                        );

                        val rowId = repository.insertOrUpdate(downloadTask);
                        if (rowId != -1L) {
                            downloadTask = downloadTask.copy(id = rowId.toInt());
                            startDownload(downloadTask);
                        } else {
                            throw Exception("Failed to add to database")
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to get file meta data", Toast.LENGTH_SHORT)
                        .show();

                }

            } catch (e: Exception) {
                //Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                Log.d("DownloadManager", "Exception: ${e.toString()}")

            } finally {
                _isLoading.value = false;
            }
        }
    }

    private fun setIsLoading(value: Boolean, task: DownloadTask) {
        val updatedTask = task.copy(isLoading = value);
        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
            this[task.id] = updatedTask;
        };
    }

    private fun setMessage(value: String, task: DownloadTask) {
        val updatedTask = task.copy(message = value);
        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
            this[task.id] = updatedTask;
        };
    }

    private fun canStartDownload(): Boolean {
        val count = _activeDownloads.value.values.count { it.status == DownloadStatus.ACTIVE };

        if (count < maxParallelDownloads) return true;

        return false;
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun startDownload(task: DownloadTask) {

        if (!canStartDownload()) {
            val updatedTask = task.copy(status = DownloadStatus.PAUSED);
            _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                this[task.id] = updatedTask;

            }
            coroutineScope.launch {
                repository.insertOrUpdate(updatedTask);
            }
            return;
        }

        setMessage("", task);

        Log.d("DownloadManager", "start downloading ${task.status}")

        activeDownloadJobs[task.id]?.cancel();

        var updatedTask = task.copy();

        val downloadJob = coroutineScope.launch {
            try {

                setIsLoading(true, task);

                Log.d("DownloadManager", "before file uri");

                val cacheDir =
                    context.externalCacheDir ?: throw Exception("Failed to get cache dir")

                val file = File(cacheDir, task.fileName);

                if (!file.exists()) {
                    Log.d("DownloadManager", "File does not exist");
                }

                var downloadedBytes = if (file.exists() && task.supportsResume) file.length() else 0L;

                if (file.exists()) {
                    Log.d("DownloadManager", "File size: ${file.length()} ${file.totalSpace}")
                }

                Log.d("DownloadManager", "setting to active");

                val request = Request.Builder().url(task.url).header(
                    userAgent,
                    userAgentValue,
                ).apply {
                    if (downloadedBytes > 0 && task.supportsResume) {
                        header("Range", "bytes=$downloadedBytes-")
                        Log.d("DownloadManager", "range : $downloadedBytes")
                    }
                }.build()

                //Log.d("DownloadManager", "bytes=$downloadedBytes-");

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Failed to download file")

                Log.d("DownloadManager", "Response Code: ${response.code}")
                Log.d("DownloadManager", "Content-Range: ${response.header("Content-Range")}")

                val totalBytes = task.totalBytes;

                val outputStream = FileOutputStream(file, task.supportsResume);

                val sink = outputStream.sink().buffer();
                val source = response.body?.source() ?: throw Exception("Empty response body")

                setIsLoading(false, task);

                var progressInt = getFloor(updatedTask.progress);

                notificationManager.showDownloadNotification(
                    updatedTask.id,
                    updatedTask.fileName,
                    progress = progressInt,
                );

                //Log.d("DownloadManager", "updated Task status: ${updatedTask.status}");

                while (true) {

                    val startTime = System.currentTimeMillis();

                    //Log.d("DownloadManager", "updated Task: ${updatedTask.status}");

                    if (activeDownloadJobs[task.id]?.isCancelled == true) {
                        Log.d("DownloadManager", "break download");
                        break;
                    }

                    val bytesRead = source.read(sink.buffer, 8192);
                    if (bytesRead == -1L) break
                    sink.emit();
                    downloadedBytes += bytesRead

                    val endTime = System.currentTimeMillis();
                    val elapsedTime = (endTime - startTime) / 1000.0;

//                    Log.d(
//                        "DownloadManager",
//                        "timeeee: $startTime $endTime $elapsedTime ${updatedTask.status}"
//                    )

                    var speed = formatSpeed(0.0);

                    if (elapsedTime > 0) {
                        speed = formatSpeed((bytesRead / elapsedTime).toDouble());
                    }

                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100;

                    val tempProgress = getFloor(updatedTask.progress);

                    if(tempProgress > progressInt){
                        progressInt = tempProgress
                        notificationManager.showDownloadNotification(
                            updatedTask.id,
                            updatedTask.fileName,
                            progress = progressInt,
                        );
                    }

                    repository.updateTaskProgress(
                        task.id,
                        progress,
                        DownloadStatus.ACTIVE,
                        downloadedBytes,
                    );

                    //Log.d("DownloadManager", "setting to active 3");

                    updatedTask =
                        updatedTask.copy(
                            progress = progress,
                            downloadedBytes = downloadedBytes,
                            speed = speed,
                        );

                    Log.d("DownloadManger", "loop progesss: $progress ${updatedTask.progress}")

                    _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                        this[task.id] = updatedTask
                    }

                }

                sink.flush();
                //fileDescriptor?.fileDescriptor?.sync()
                sink.close();
                //Log.d("DownloadManager", "outputStream closed");
                source.close();
                //fileDescriptor?.close();
                //Log.d("DownloadManager", "source closed");

                updatedTask = updatedTask.copy(status = DownloadStatus.COMPLETED, speed = "")

                saveFileToDownloads(context, file, updatedTask.mimeType);

                file.delete();

                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    remove(updatedTask.id)
                }

                _completedDownloads.value = _completedDownloads.value.toMutableMap().apply {
                    this[updatedTask.id] = updatedTask
                }

                //notificationManager.cancelNotification(updatedTask.id);

                notificationManager.showDownloadCompleteNotification(
                    updatedTask.id,
                    updatedTask.fileName,
                );

                repository.updateTaskProgress(
                    task.id, 100f, DownloadStatus.COMPLETED, downloadedBytes
                );

                //Log.d("DownloadManager", "setting to complete");

                activeDownloadJobs.remove(task.id);

            } catch (e: Exception) {
                setIsLoading(false, updatedTask);
                Log.d("DownloadManager", "error: ${e.toString()}")
                repository.updateTaskProgress(
                    updatedTask.id,
                    updatedTask.progress,
                    DownloadStatus.PAUSED,
                    updatedTask.downloadedBytes
                )

                updatedTask =
                    updatedTask.copy(
                        status = DownloadStatus.PAUSED,
                        speed = "",
                        message = e.message ?: "Error"
                    )
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[task.id] = updatedTask
                }

                notificationManager.cancelNotification(updatedTask.id);

                //Log.d("DownloadManager", "setting to cancel");
            }
        }

        activeDownloadJobs[task.id] = downloadJob;
    }

    fun pauseDownload(taskId: Int) {
        notificationManager.cancelNotification(taskId);
        _activeDownloads.value[taskId]?.let { task ->
            coroutineScope.launch {

                val updatedTask = task.copy(status = DownloadStatus.PAUSED, speed = "")
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[taskId] = updatedTask
                }

                Log.d("DownloadManager", "setting to paused : ${task.progress}");

                activeDownloadJobs[taskId]?.takeIf { it.isActive }?.cancel()

                activeDownloadJobs.remove(taskId)

                val cache = context.externalCacheDir;
                val file = File(cache, task.fileName);

                repository.updateTaskProgress(
                    taskId, task.progress, DownloadStatus.PAUSED, file.length()
                )



            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun resumeDownload(task: DownloadTask) {
        val updatedTask = task.copy(status = DownloadStatus.ACTIVE)
        //Log.d("DownloadManager", "resume updated Task: ${updatedTask.status}");
        startDownload(updatedTask);
    }

    fun cancelDownload(taskId: Int) {
        notificationManager.cancelNotification(taskId);
        _activeDownloads.value[taskId]?.let { task ->

            activeDownloadJobs[taskId]?.takeIf { it.isActive }?.cancel()

            activeDownloadJobs.remove(taskId)

            coroutineScope.launch {
                repository.updateTaskProgress(
                    taskId, task.progress, DownloadStatus.CANCELLED, task.downloadedBytes
                )

                val updatedTask = task.copy(status = DownloadStatus.CANCELLED)
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[taskId] = updatedTask
                }

                Log.d("DownloadManager", "setting to cancel 2");

            }
        }
    }
}
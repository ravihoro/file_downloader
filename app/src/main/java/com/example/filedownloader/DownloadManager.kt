package com.example.filedownloader

import android.content.Context
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val repository: DownloadTaskRepository,
    @ApplicationContext private val context: Context,
) {

    private val client =
        OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true).followSslRedirects(true).build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeDownloads = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap())
    val activeDownloads: StateFlow<Map<Int, DownloadTask>> get() = _activeDownloads

    private val _completedDownloads = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap());
    val completedDownloads: StateFlow<Map<Int, DownloadTask>> = _completedDownloads;

    private val activeDownloadJobs = mutableMapOf<Int, Job>()

    private val userAgent = "User-Agent";
    private val userAgentValue =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    fun getActiveDownloadsFromDb() {
        coroutineScope.launch {
            val activeFlow = repository.getTasksByStatus(DownloadStatus.ACTIVE)
            val pausedFlow = repository.getTasksByStatus(DownloadStatus.PAUSED)

            combine(activeFlow, pausedFlow) { activeList, pausedList ->
                val activeDownloadsFromDb = activeList.associateBy { it.id }
                val pausedDownloadsFromDb = pausedList.associateBy { it.id }
                activeDownloadsFromDb + pausedDownloadsFromDb
            }.collect { combinedDownloads ->
                _activeDownloads.emit(combinedDownloads)
            }
        }
    }

    fun getCompletedDownloadsFromDb() {
        coroutineScope.launch {
            repository.getTasksByStatus(DownloadStatus.COMPLETED).collect { response ->
                val completedDownloadsFromDb = response.associateBy { it.id }
                _completedDownloads.emit(completedDownloadsFromDb);
            };
        }
    }

    var _isLoading = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = _isLoading;

    fun getFileMetaData(url: String) {
        coroutineScope.launch {
            try {
                _isLoading.value = true;
                val request = Request.Builder().url(url).header(
                    userAgent,
                    userAgentValue,
                ).head().build()

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

                    var downloadTask = DownloadTask(
                        fileName = fileName,
                        url = url,
                        totalBytes = contentLength,
                        progress = 0f,
                        mimeType = contentType,
                    );

                    Log.d("DownloadManager", "created task: ${downloadTask.fileName} ${downloadTask.url} ${downloadTask.totalBytes} ${downloadTask.mimeType}");


                    val rowId = repository.insertOrUpdate(downloadTask);
                    if (rowId != -1L) {
                        downloadTask = downloadTask.copy(id = rowId.toInt());
                        startDownload(downloadTask);
                    } else {
                        throw Exception("Failed to add to database")
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

    fun startDownload(task: DownloadTask) {
        Log.d("DownloadManager", "start downloading ${task.status}")

        activeDownloadJobs[task.id]?.cancel();

        var downloadJob = coroutineScope.launch {
            try {

                _isLoading.value = true;

                repository.getTaskById(task.id) ?: throw Exception("Error: Cannot find task")

                val fileUri = getFileUri(task.fileName, task.mimeType, context)
                    ?: throw Exception("Failed to create file");

                //Log.d("DownloadManager", "File uri: $fileUri");

                val file  = File(fileUri.path!!)

                //Log.d("DownloadManager", "start download path: ${file.exists()} ${file.path}");

                val outputStream = context.contentResolver.openOutputStream(fileUri)
                    ?: throw Exception("Failed to get output stream")

                //Log.d("DownloadManager", "Output stream opened successfully")

                var downloadedBytes = getDownloadedBytes(file)

                //Log.d("DownloadManager", "downloaded bytes: $downloadedBytes");

                val progress = getProgress(file, task.totalBytes)

                //Log.d("DownloadManager", "Progress initial: $progress");

                repository.updateTaskProgress(
                    task.id,
                    progress,
                    DownloadStatus.ACTIVE,
                    task.totalBytes ?: 0L
                )

                Log.d("DownloadManager", "setting to active");

                val request = Request.Builder().url(task.url)
                    .header(
                        userAgent,
                        userAgentValue,
                    )
                    .apply {
                        if (downloadedBytes > 0) {
                            header("Range", "bytes=$downloadedBytes-")
                        }
                    }.build()

                //Log.d("DownloadManager", "bytes=$downloadedBytes-");

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Failed to download file")

                val totalBytes =
                    task.totalBytes ?: response.headers["Content-Range"]?.substringAfter("/")
                        ?.toLongOrNull() ?: (response.body?.contentLength()
                        ?.let { it + downloadedBytes }
                        ?: throw Exception("Unable to determine file size"))


                repository.updateTaskProgress(task.id, 0f, DownloadStatus.ACTIVE, totalBytes)
                //Log.d("DownloadManager", "setting to active 2");


                val sink = outputStream.sink().buffer();
                val source = response.body?.source() ?: throw Exception("Empty response body")

                _isLoading.value = false;

                //Log.d("DownloadManager", "task status: ${task.status}");

                var updatedTask = task.copy(progress = progress)

                //Log.d("DownloadManager", "updated Task status: ${updatedTask.status}");

                while (true) {

                    //Log.d("DownloadManager", "updated Task: ${updatedTask.status}");

                    if (activeDownloadJobs[updatedTask.id]?.isCancelled == true) {
                        Log.d("DownloadManager", "break download");

                        break;
                    }

                    val bytesRead = source.read(sink.buffer, 8192);
                    if (bytesRead == -1L) break
                    downloadedBytes += bytesRead

                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100;

                    repository.updateTaskProgress(
                        updatedTask.id,
                        progress,
                        DownloadStatus.ACTIVE,
                        totalBytes
                    );

                    //Log.d("DownloadManager", "setting to active 3");

                    updatedTask = updatedTask.copy(progress = progress);

                    _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                        this[task.id] = updatedTask
                    }
                }


                sink.close();
                //Log.d("DownloadManager", "outputStream closed");
                source.close();
                //Log.d("DownloadManager", "source closed");

                updatedTask = updatedTask.copy(status = DownloadStatus.COMPLETED)

                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    remove(updatedTask.id)
                }

                _completedDownloads.value = _completedDownloads.value.toMutableMap().apply {
                    this[updatedTask.id] = updatedTask
                }

                repository.updateTaskProgress(task.id, 100f, DownloadStatus.COMPLETED, totalBytes);

                //Log.d("DownloadManager", "setting to complete");

                activeDownloadJobs.remove(task.id);

            } catch (e: Exception) {
                _isLoading.value = false;
                //Log.d("DownloadManager", "error: ${e.toString()}")
                repository.updateTaskProgress(
                    task.id,
                    0f,
                    DownloadStatus.CANCELLED,
                    task.totalBytes ?: 0L
                )

                val updatedTask = task.copy(status = DownloadStatus.CANCELLED)
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[task.id] = updatedTask
                }

                //Log.d("DownloadManager", "setting to cancel");
            }
        }

        activeDownloadJobs[task.id] = downloadJob;
    }

    fun pauseDownload(taskId: Int) {
        _activeDownloads.value[taskId]?.let { task ->
            coroutineScope.launch {

                activeDownloadJobs[taskId]?.cancel();
                activeDownloadJobs.remove(taskId)

                repository.updateTaskProgress(
                    taskId,
                    task.progress,
                    DownloadStatus.PAUSED,
                    task.totalBytes ?: 0L
                )

                val updatedTask = task.copy(status = DownloadStatus.PAUSED)
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[taskId] = updatedTask
                }

                //Log.d("DownloadManager", "setting to paused");

            }
        }
    }

    fun resumeDownload(task: DownloadTask) {
        var updatedTask = task.copy(status = DownloadStatus.ACTIVE)
        //Log.d("DownloadManager", "resume updated Task: ${updatedTask.status}");
        startDownload(updatedTask);
    }

    fun cancelDownload(taskId: Int) {
        _activeDownloads.value[taskId]?.let { task ->

            activeDownloadJobs[taskId]?.cancel();
            activeDownloadJobs.remove(taskId)

            coroutineScope.launch {
                repository.updateTaskProgress(
                    taskId,
                    task.progress,
                    DownloadStatus.CANCELLED,
                    task.totalBytes ?: 0L
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
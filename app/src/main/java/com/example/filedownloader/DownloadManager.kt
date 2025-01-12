package com.example.filedownloader

import android.content.Context
import android.util.Log
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
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

                    // Extract file name from Content-Disposition or URL
                    val fileName =
                        if (contentDisposition != null && contentDisposition.contains("filename=")) {
                            contentDisposition.substringAfter("filename=").trim('"')
                        } else {
                            url.substringAfterLast("/")
                        }

                    val file = getFile(fileName, context);

                    val progress = getProgress(file, contentLength)

                    var downloadTask = DownloadTask(
                        fileName = fileName,
                        url = url,
                        totalBytes = contentLength,
                        progress = progress,
                    );

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
                Toast.makeText(context, "", Toast.LENGTH_SHORT).show()
                Log.d("DownloadManager", "Exception: ${e.toString()}")

            } finally {
                _isLoading.value = false;
            }
        }


    }

    fun startDownload(task: DownloadTask) {
        Log.d("DownloadManager", "downloading")
        coroutineScope.launch {
            try {

                _isLoading.value = true;

                repository.getTaskById(task.id) ?: throw Exception("Error: Cannot find task")

                val file = getFile(task.fileName, context);

                var downloadedBytes = getDownloadedBytes(file)

                val progress = getProgress(file, task.totalBytes)

                repository.updateTaskProgress(
                    task.id,
                    progress,
                    DownloadStatus.ACTIVE,
                    task.totalBytes ?: 0L
                )

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


                val response = client.newCall(request).execute()

                if (!response.isSuccessful) throw Exception("Failed to download file")

                val totalBytes =
                    task.totalBytes ?: response.headers["Content-Range"]?.substringAfter("/")
                        ?.toLongOrNull() ?: (response.body?.contentLength()
                        ?.let { it + downloadedBytes }
                        ?: throw Exception("Unable to determine file size"))


                repository.updateTaskProgress(task.id, 0f, DownloadStatus.ACTIVE, totalBytes)


                val sink = file.sink(append = true).buffer();
                val source = response.body?.source() ?: throw Exception("Empty response body")

                _isLoading.value = false;

                while (true) {
                    val bytesRead = source.read(sink.buffer, 8192)
                    if (bytesRead == -1L) break
                    downloadedBytes += bytesRead

                    val progress = (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100;

                    repository.updateTaskProgress(
                        task.id,
                        progress,
                        DownloadStatus.ACTIVE,
                        totalBytes
                    );

                    val updatedTask = task.copy(progress = progress)
                    _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                        this[task.id] = updatedTask
                    }
                }

                sink.close();
                source.close();

                val updatedTask = task.copy(status = DownloadStatus.COMPLETED)
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    this[task.id] = updatedTask
                }

                repository.updateTaskProgress(task.id, 100f, DownloadStatus.COMPLETED, totalBytes);


            } catch (e: Exception) {
                _isLoading.value = false;
                Log.d("DownloadManager", e.toString())
                repository.updateTaskProgress(
                    task.id,
                    0f,
                    DownloadStatus.CANCELLED,
                    task.totalBytes ?: 0L
                )
            }
        }
    }

    fun pauseDownload(taskId: Int) {
        _activeDownloads.value[taskId]?.let { task ->
            coroutineScope.launch {
                _activeDownloads.value -= taskId
                repository.updateTaskProgress(
                    taskId,
                    task.progress,
                    DownloadStatus.PAUSED,
                    task.totalBytes ?: 0L
                )
            }
        }
    }

    fun resumeDownload(task: DownloadTask) {
        startDownload(task);
    }

    fun cancelDownload(taskId: Int) {
        _activeDownloads.value[taskId]?.let { task ->
            coroutineScope.launch {
                _activeDownloads.value -= taskId
                repository.updateTaskProgress(
                    taskId,
                    task.progress,
                    DownloadStatus.CANCELLED,
                    task.totalBytes ?: 0L
                )
            }
        }
    }
}
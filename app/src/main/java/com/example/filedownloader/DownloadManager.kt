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
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadManager @Inject constructor(
    private val repository: DownloadTaskRepository,
    @ApplicationContext private val context: Context,
) {

    private val client =
        OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeDownloads = MutableStateFlow<Map<Int, DownloadTask>>(emptyMap())

    val activeDownloads: StateFlow<Map<Int, DownloadTask>> get() = _activeDownloads

    var _isLoading = MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> = _isLoading;

    fun startDownload(task: DownloadTask) {
        Log.d("DownloadManager", "downloading")
        val downloadJob = coroutineScope.launch {
            try {

                _isLoading.value = true;

                val taskInDb = repository.getTaskById(task.id)

                if (taskInDb == null) {
                    Log.d("DownloadManager", "inserting: ${task.fileName} ${task.status} ${task.progress}")
                    val taskId = repository.insertOrUpdate(task);

                    if(taskId == -1L){
                        Toast.makeText(context, "Failed to add task", Toast.LENGTH_SHORT).show()
                        throw Exception("Failed to insert task")
                    }

                    val updatedTask = task.copy(id = taskId.toInt());

                    val updatedActiveDownloads = _activeDownloads.value.toMutableMap()
                    updatedActiveDownloads[task.id] = updatedTask
                    _activeDownloads.value = updatedActiveDownloads
                }

                if (taskInDb != null && taskInDb.status == DownloadStatus.COMPLETED) {
                    _isLoading.value = false;
                    Toast.makeText(context, "File already downloaded", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val file = File(context.getExternalFilesDir(null), task.fileName);

                var downloadedBytes = if (file.exists()) file.length() else 0L

                repository.updateTaskProgress(
                    task.id,
                    if (task.totalBytes != null && task.totalBytes > 0) {
                        (downloadedBytes.toFloat() / task.totalBytes.toFloat()) * 100
                    } else 0f, DownloadStatus.ACTIVE,
                    task.totalBytes ?: 0L
                )

                val request = Request.Builder().url(task.url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
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
//        _activeDownloads.value[taskId]?.cancel()
//        coroutineScope.launch {
//            val task = repository.getTaskById(taskId)
//            if (task != null) {
//                repository.updateTaskProgress(
//                    taskId,
//                    0f,
//                    DownloadStatus.PAUSED,
//                    task.totalBytes ?: 0L
//                );
//            }
//        }
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
//        _activeDownloads.value[taskId]?.cancel()
//        coroutineScope.launch {
//            val task = repository.getTaskById(taskId)
//            if (task != null) {
//                repository.updateTaskProgress(
//                    taskId,
//                    0f,
//                    DownloadStatus.CANCELLED,
//                    task.totalBytes ?: 0L
//                )
//                _activeDownloads.value -= taskId;
//            }
//        }
    }

}
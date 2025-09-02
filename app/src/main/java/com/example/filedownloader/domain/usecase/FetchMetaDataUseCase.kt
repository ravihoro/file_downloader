package com.example.filedownloader.domain.usecase

import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.repository.DownloadTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class FetchMetaDataUseCase @Inject constructor(
    private val repository: DownloadTaskRepository,
    private val client: OkHttpClient,
) {

    private val userAgent = "User-Agent"
    private val userAgentValue =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

    suspend operator fun invoke(url: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head() // HEAD request just for metadata
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch metadata: ${response.code}"))
            }

            val headers = response.headers
            val contentDisposition = headers["Content-Disposition"]
            val contentLength = headers["Content-Length"]?.toLongOrNull() ?: 0L
            val contentType = headers["Content-Type"] ?: "application/octet-stream"

            val fileName = if (contentDisposition?.contains("filename=") == true) {
                contentDisposition.substringAfter("filename=").trim('"')
            } else {
                url.substringAfterLast("/")
            }

            // Check if already exists
            val existingTask = repository.getTaskByFileNameAndMimeType(fileName, contentType)
            if (existingTask != null) {
                return@withContext Result.success(existingTask.id.toLong())
            }

            val task = DownloadTask(
                fileName = fileName,
                url = url,
                totalBytes = contentLength,
                mimeType = contentType,
                progress = 0f,
                status = DownloadStatus.PAUSED, // not yet downloading
                supportsResume = response.code == 206 && response.header("Content-Range") != null
            )

            val id = repository.insertOrUpdate(task)
            if (id == -1L) {
                Result.failure(Exception("DB insert failed"))
            } else {
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
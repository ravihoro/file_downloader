package com.example.filedownloader.data.repository

import com.example.filedownloader.data.remote.RemoteFileMeta
import okhttp3.OkHttpClient
import javax.inject.Inject
import okhttp3.Request

class RemoteMetaDataRepository @Inject constructor(
    private val client: OkHttpClient
){

    private val userAgent = "User-Agent"
    private val userAgentValue =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"


    fun fetchMetaData(url: String): Result<RemoteFileMeta> {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header(userAgent, userAgentValue)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Failed metadata fetch: ${response.code}"))
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

            val fileMeta = RemoteFileMeta(
                url = url,
                fileName = fileName,
                contentLength = contentLength,
                contentType = contentType,
                supportsResume = response.header("Accept-Ranges") == "bytes"
            )

            Result.success(fileMeta)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}
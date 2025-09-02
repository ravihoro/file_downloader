package com.example.filedownloader.data.repository

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class RemoteDownloadDataRepository @Inject constructor(
    private val client: OkHttpClient
) {
    fun startDownload(url: String, downloadedBytes : Long, supportsResume: Boolean): Result<Response> {

        return try {
            val requestBuilder = Request.Builder().url(url)

            if(downloadedBytes > 0 && supportsResume){
                requestBuilder.header("Range", "bytes=$downloadedBytes-")
            }

            val request = requestBuilder.build()

            val response = client.newCall(request).execute()

            if(!response.isSuccessful) {
                 Result.failure(Exception("Failed to get response"))
            }else{
                 Result.success(response)
            }
        }catch(e: Exception){
            Result.failure(e)
        }
    }
}
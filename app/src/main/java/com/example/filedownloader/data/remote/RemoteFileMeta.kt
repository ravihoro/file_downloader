package com.example.filedownloader.data.remote

data class RemoteFileMeta(
    val url: String,
    val fileName: String,
    val contentLength: Long,
    val contentType: String,
    val supportsResume: Boolean,
)

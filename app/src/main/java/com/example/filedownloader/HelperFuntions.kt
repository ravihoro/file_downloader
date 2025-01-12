package com.example.filedownloader

import android.content.Context
import java.io.File

fun getFile(fileName: String, context: Context) : File {
    return File(context.getExternalFilesDir(null), fileName);
}

fun getDownloadedBytes(file: File) : Long {
    return if (file.exists()) file.length() else 0L
}

fun getProgress(file: File, totalBytes: Long?) : Float {
    val downloadedBytes = if (file.exists()) file.length() else 0L

    val progress = if (totalBytes != null && totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100
    } else 0f

    return progress;
}
package com.example.filedownloader.utils


import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream
import kotlin.math.floor
import kotlin.text.*

@RequiresApi(Build.VERSION_CODES.Q)
fun saveFileToDownloads(context: Context, file: File, mimeType: String) {
    val resolver = context.contentResolver;

    val contentValues = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, file.name)
        put(MediaStore.Downloads.MIME_TYPE, mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
    if(uri != null){
        try {
            resolver.openOutputStream(uri).use { outputStream ->
                FileInputStream(file).use { inputStream ->
                    inputStream.copyTo(outputStream!!)
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to save the file: ${e.message}", e)
        }
    }else{
        throw Exception("Uri is nullll")
    }
}


fun formatBytes(bytes: Long): String {
    val kilobyte = 1024
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024

    return when {
        bytes >= gigabyte -> String.format("%.1f GB", bytes.toDouble() / gigabyte)
        bytes >= megabyte -> String.format("%.1f MB", bytes.toDouble() / megabyte)
        bytes >= kilobyte -> String.format("%.1f KB", bytes.toDouble() / kilobyte)
        else -> "$bytes B"
    }
}

fun formatSpeed(bytesPerSecond: Double): String {
    val kilobyte = 1024
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024

    return when {
        bytesPerSecond >= gigabyte -> String.format("%.1f GB/s", bytesPerSecond / gigabyte)
        bytesPerSecond >= megabyte -> String.format("%.1f MB/s", bytesPerSecond / megabyte)
        bytesPerSecond >= kilobyte -> String.format("%.1f KB/s", bytesPerSecond / kilobyte)
        else -> String.format("%.1f B/s", bytesPerSecond)
    }
}

fun getFloor(value: Float): Int {
    return floor(value).toInt();
}
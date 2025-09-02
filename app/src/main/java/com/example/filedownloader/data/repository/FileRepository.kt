package com.example.filedownloader.data.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import javax.inject.Inject
import java.io.FileInputStream

class FileRepository @Inject constructor(
    private val context: Context,
) {

    fun getCacheFile(fileName: String): File {
        val cacheDir = context.externalCacheDir ?: context.cacheDir
        return File(cacheDir, fileName)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveFileToDownloads(context: Context, file: File, mimeType: String): Boolean {
        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        return if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    FileInputStream(file).use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
}
package com.example.filedownloader

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileInputStream

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

fun getFileUri(fileName: String, mimeType: String, context: Context, isTaskInDb: Boolean): Uri? {

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        val projection = arrayOf(MediaStore.Downloads._ID);
        val selection =
            "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.MIME_TYPE}=?"
        val selectionArgs = arrayOf(fileName, mimeType);

        val cursor = context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        );

        if (cursor != null && cursor.moveToFirst() && isTaskInDb) {
            Log.d("DownloadManager", "iffff")
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
            cursor.close();
            Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString());
        } else {
            Log.d("DownloadManager", "elseeeee")
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            );
        }
    } else {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) downloadsDir.mkdirs();

        val file = File(downloadsDir, fileName);
        if (!file.exists()) file.createNewFile();
        Uri.fromFile(file)
    }
}

fun getFileNameFromUri(context: Context, fileUri: Uri): String? {
    val projection = arrayOf(MediaStore.Downloads.DISPLAY_NAME);

    context.contentResolver.query(fileUri, projection, null, null, null)?.use {cursor ->
        if(cursor.moveToFirst()){
            return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
        }
    }

    return null
}

fun getFileSizeFromUri(uri: Uri, context: Context): Long {
    var size: Long = 0
    val contentResolver: ContentResolver = context.contentResolver

    if (uri.scheme == "content") {
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
    } else if (uri.scheme == "file") {
        // If the URI is a file path
        val file = File(uri.path ?: "")
        size = file.length()
    }

    return size
}

fun getDownloadedBytes(file: File): Long {
    Log.d("DownloadManager", "File exists: ${file.exists()} ${file.length()}");
    return if (file.exists()) file.length() else 0L
}

fun getProgress(downloadedBytes: Long, totalBytes: Long?): Float {

    val progress = if (totalBytes != null && totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100
    } else 0f

    return progress;
}
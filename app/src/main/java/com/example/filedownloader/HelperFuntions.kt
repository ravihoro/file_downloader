package com.example.filedownloader

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

fun getFileUri(fileName: String, mimeType: String,  context: Context) : Uri? {
    //return File(context.getExternalFilesDir(null), fileName);

    return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);

        uri
    }else{
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if(!downloadsDir.exists()) downloadsDir.mkdirs();

        val file = File(downloadsDir, fileName);
        if(!file.exists()) file.createNewFile();
        Uri.fromFile(file)
    }

}

fun getDownloadedBytes(file: File) : Long {
    Log.d("DownloadManager", "File exists: ${file.exists()} ${file.length()}");
    return if (file.exists()) file.length() else 0L
}

fun getProgress(file: File, totalBytes: Long?) : Float {
    val downloadedBytes = if (file.exists()) file.length() else 0L

    val progress = if (totalBytes != null && totalBytes > 0) {
        (downloadedBytes.toFloat() / totalBytes.toFloat()) * 100
    } else 0f


    return progress;
}
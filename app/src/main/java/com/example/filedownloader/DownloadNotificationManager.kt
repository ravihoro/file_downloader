package com.example.filedownloader

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@Singleton
class DownloadNotificationManager(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "download_channel";
        private const val CHANNEL_NAME = "Download Notification";
        private const val CHANNEL_DESCRIPTION = "Shows notification for active downloads";
    }

    init {
        createNotificationChannel();
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = CHANNEL_DESCRIPTION
            };

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

            notificationManager.createNotificationChannel(channel);
        }
    }

    fun showDownloadNotification(taskId: Int, title: String, progress: Int) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading: ${title}")
                .setContentText("Progress: ${progress}%")
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            NotificationManagerCompat.from(context).notify(taskId, notification);
        }
    }

    fun showDownloadCompleteNotification(taskId: Int, title: String) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Download Complete: $title")
                .setContentText("File downloaded successfully")
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            NotificationManagerCompat.from(context).notify(taskId, notification);
        }

    }

    fun cancelNotification(taskId: Int) {
        NotificationManagerCompat.from(context).cancel(taskId);
    }


}
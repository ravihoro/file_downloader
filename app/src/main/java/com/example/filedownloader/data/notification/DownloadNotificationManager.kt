package com.example.filedownloader.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import javax.inject.Singleton
import android.app.Notification
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Singleton
class DownloadNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
){
    companion object {
        private const val ONGOING_CHANNEL_ID = "download_ongoing"
        private const val ONGOING_CHANNEL_NAME = "Active Downloads"

        private const val COMPLETE_CHANNEL_ID = "download_complete"
        private const val COMPLETE_CHANNEL_NAME = "Completed Downloads"

        private const val GROUP_KEY_DOWNLOADS = "downloads_group"
    }

    init {
        createNotificationChannel();
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ongoing = NotificationChannel(
                ONGOING_CHANNEL_ID,
                ONGOING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows ongoing download progress" }

            val complete = NotificationChannel(
                COMPLETE_CHANNEL_ID,
                COMPLETE_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when download is complete" }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

            notificationManager.createNotificationChannel(ongoing)
            notificationManager.createNotificationChannel(complete)
        }
    }

    fun createDownloadNotification(taskId: Int, title: String, progress: Int): Notification {
        return NotificationCompat.Builder(context, ONGOING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading: $title")
            .setContentText("Progress: $progress%")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun showDownloadCompleteNotification(taskId: Int, title: String) {
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val notification = NotificationCompat.Builder(context, COMPLETE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
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
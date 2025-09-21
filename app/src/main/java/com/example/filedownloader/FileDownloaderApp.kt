package com.example.filedownloader

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.filedownloader.data.manager.InAppDownloadManager
import com.example.filedownloader.data.notification.DownloadNotificationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class FileDownloaderApp: Application() {

    @Inject lateinit var inAppDownloadManager: InAppDownloadManager
    @Inject lateinit var notificationManager: DownloadNotificationManager

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            inAppDownloadManager.reconcileOnStartup(resumeActiveToAuto = false)
        }
    }
}
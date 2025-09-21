package com.example.filedownloader.data.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.concurrent.ConcurrentHashMap

class DownloadForegroundService: Service() {
    companion object {

        private const val GROUP_KEY_DOWNLOADS = "downloads_group"
        private const val SERVICE_SUMMARY_ID = 9000

        const val ACTION_START = "download.action.START"
        const val ACTION_UPDATE = "download.action.UPDATE"
        const val ACTION_STOP = "download.action.STOP"
        const val EXTRA_TASK_ID = "extra.task_id"
        const val ACTION_UPDATE_SUMMARY = "download.action.UPDATE_SUMMARY"

        fun startServiceForTask(context: Context){

            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
            }

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(intent)
            }else{
                context.startService(intent)
            }

        }

        fun updateNotification(context: Context, taskId: Int, notification: Notification){
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TASK_ID, taskId)
            }

            NotificationStore.put(taskId, notification)

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                context.startForegroundService(intent)
            }else{
                context.startService(intent)
            }
        }

        fun stopIfIdle(context: Context){
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_STOP
            }

            context.startService(intent)
        }

        fun updateServiceSummary(context: Context){
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE_SUMMARY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }

    object NotificationStore {
        private val map = ConcurrentHashMap<Int, Notification>()
        fun put(taskId: Int, n: Notification) = map.put(taskId, n)
        fun remove(taskId: Int) = map.remove(taskId)
        fun get(taskId: Int) = map[taskId]
        fun getAny(): Notification? = map.values.firstOrNull()
        fun mapSize(): Int = map.size
        fun clear() = map.clear()
    }

    private val binder = LocalBinder()

    inner class LocalBinder: Binder() {
        fun getService() = this@DownloadForegroundService
    }

    override fun onBind(p0: Intent?) = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action){
            ACTION_START -> {
                val n = buildSummaryNotification()
                startForeground(SERVICE_SUMMARY_ID, n)
            }
            ACTION_UPDATE -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val n = if (taskId != -1) NotificationStore.get(taskId) else null
                if (taskId != -1 && n != null) {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(taskId, n) // update per-task notification
                    nm.notify(SERVICE_SUMMARY_ID, buildSummaryNotification()) // always update summary
                } else {
                    // Defensive: we expected the per-task notification to exist in store
                    Log.w(TAG, "ACTION_UPDATE: no per-task notification found for taskId=$taskId")
                    // still refresh summary based on current store
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(SERVICE_SUMMARY_ID, buildSummaryNotification())
                }
            }
            ACTION_STOP -> {
                try {
                    stopForeground(true)
                } catch (t: Throwable) {
                    Log.w(TAG, "stopForeground threw: ${t.message}")
                }
                NotificationStore.clear()
                stopSelf()
            }
            ACTION_UPDATE_SUMMARY -> {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(SERVICE_SUMMARY_ID, buildSummaryNotification())
            }
        }

        return START_STICKY
    }

    private fun buildSummaryNotification(): Notification {
        val activeCount = NotificationStore.mapSize()
        val contentText = if (activeCount > 0) {
            "$activeCount active download${if (activeCount > 1) "s" else ""}"
        } else {
            "Managing downloads"
        }

        return NotificationCompat.Builder(this, "download_ongoing")
            .setContentTitle("Downloads")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setGroupSummary(true)
            .build()

    }
}
package com.example.filedownloader

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DownloadTask")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val url: String,
    val progress: Float = 0.0f,
    val status: DownloadStatus = DownloadStatus.ACTIVE,
    val totalBytes: Long? = null,
    val mimeType: String = "",
)

enum class DownloadStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

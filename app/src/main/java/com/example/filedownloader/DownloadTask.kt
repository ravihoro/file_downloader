package com.example.filedownloader

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DownloadTask")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val url: String,
    val downloadedBytes: Long = 0L,
    val progress: Float = 0.0f,
    val status: DownloadStatus = DownloadStatus.ACTIVE,
    val speed: String = "",
    val totalBytes: Long = 0L,
    val mimeType: String = "",
    val isLoading: Boolean = false,
    val message: String = "",
)

enum class DownloadStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
}

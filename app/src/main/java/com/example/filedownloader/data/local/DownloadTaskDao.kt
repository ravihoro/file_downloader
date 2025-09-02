package com.example.filedownloader.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {

    @Query("SELECT * FROM DownloadTask")
    fun getAllTasks() : Flow<List<DownloadTask>>

    @Query("SELECT * FROM DownloadTask WHERE status = :status")
    fun getTaskByStatus(status: DownloadStatus): Flow<List<DownloadTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(task: DownloadTask): Long

    @Query("""
        UPDATE DownloadTask
        SET progress = :progress, downloadedBytes = :downloadedBytes, status = :status, speed = :speed
        WHERE id = :id
    """)
    suspend fun updateTaskProgress(
        id: Int,
        progress: Float,
        status: DownloadStatus,
        downloadedBytes: Long,
        speed: String
    ): Int

    @Query("SELECT * FROM DownloadTask WHERE id = :id")
    suspend fun getTaskById(id: Int): DownloadTask?

    @Query("SELECT * FROM DownloadTask WHERE fileName = :fileName AND mimeType = :mimeType")
    suspend fun getTaskByFileNameAndMimeType(fileName: String, mimeType: String): DownloadTask?


    @Delete
    suspend fun deleteTask(task: DownloadTask)

}
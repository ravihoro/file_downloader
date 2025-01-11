package com.example.filedownloader

import android.util.Log
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskRepository @Inject constructor(
    private val downloadTaskDao: DownloadTaskDao)
{
    fun getTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>> {
        return downloadTaskDao.getTasksByStatusFlow(status);
    }

    suspend fun insertOrUpdate(task: DownloadTask): Long{
        val rowId = downloadTaskDao.insertOrUpdate(task);
        Log.d("DownloadManager", "rowid: $rowId")
        return rowId ;
    }

    suspend fun updateTaskProgress(id: Int, progress: Float, status: DownloadStatus, totalBytes: Long): Boolean {
        val rowsAffected = downloadTaskDao.updateTaskProgress(id, progress, status);
        return rowsAffected > 0;
    }

    suspend fun getTaskById(id: Int): DownloadTask? {
        return downloadTaskDao.getTaskById(id);
    }

    suspend fun getTaskByFileName(fileName: String): DownloadTask? {
        return downloadTaskDao.getTaskByFileName(fileName);
    }
}
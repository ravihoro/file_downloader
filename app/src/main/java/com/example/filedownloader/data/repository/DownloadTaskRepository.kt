package com.example.filedownloader.data.repository

import com.example.filedownloader.data.local.DownloadStatus
import com.example.filedownloader.data.local.DownloadTask
import com.example.filedownloader.data.local.DownloadTaskDao
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadTaskRepository @Inject constructor(
    private val downloadTaskDao: DownloadTaskDao
){

    fun getTaskByStatus(status: DownloadStatus) : Flow<List<DownloadTask>> =
        downloadTaskDao.getTaskByStatus(status)

    suspend fun insertOrUpdate(task: DownloadTask): Long =
        downloadTaskDao.insertOrUpdate(task)

    suspend fun updateTaskProgress(
        id: Int,
        progress: Float,
        status: DownloadStatus,
        downloadedBytes: Long,
    ) : Boolean = downloadTaskDao.updateTaskProgress(id, progress, status, downloadedBytes) > 0

    suspend fun getTaskById(id: Int): DownloadTask? =
        downloadTaskDao.getTaskById(id)

    suspend fun getTaskByFileNameAndMimeType(fileName: String, mimeType: String) : DownloadTask? =
        downloadTaskDao.getTaskByFileNameAndMimeType(fileName, mimeType)

    suspend fun deleteTask(task: DownloadTask) = downloadTaskDao.deleteTask(task)

}
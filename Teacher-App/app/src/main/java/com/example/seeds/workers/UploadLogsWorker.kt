package com.example.seeds.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.seeds.dao.LogDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.seeds.network.SeedsService

@HiltWorker
class UploadLogsWorker @AssistedInject constructor
    (@Assisted appContext: Context,
     @Assisted params: WorkerParameters,
     private val database: LogDao,
     private val network: SeedsService): CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val logs = database.getAll()
        if(logs.isNotEmpty()) {
            network.uploadLogs(logs)
            database.delete(logs.map { it.id })
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "UploadLogsWorker"
    }
}
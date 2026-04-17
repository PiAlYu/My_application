package com.example.storechecklist.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.storechecklist.AppGraph

class ChecklistSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            AppGraph.repository.syncWithServer()
            Result.success()
        } catch (error: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "periodic_checklist_sync"
    }
}

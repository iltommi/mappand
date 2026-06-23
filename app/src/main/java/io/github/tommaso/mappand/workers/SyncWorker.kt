package io.github.tommaso.mappand.workers

import android.content.Context
import androidx.work.*
import io.github.tommaso.mappand.MappandApp
import io.github.tommaso.mappand.domain.SyncManager
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = MappandApp.from(applicationContext)
        val syncManager = SyncManager(app.db.photoDao(), app.pCloudClient)
        return try {
            val rootId = app.pCloudClient.getRootFolderId()
            syncManager.flushIndex(rootId)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(5, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}

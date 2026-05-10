package com.blackpirateapps.brownpaper.data.wallabag

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.blackpirateapps.brownpaper.domain.repository.WallabagRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class WallabagSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, WallabagSyncWorkerEntryPoint::class.java)
            .wallabagRepository()

        return when (repository.syncNow()) {
            is WallabagSyncResult.Success -> Result.success()
            WallabagSyncResult.NotConnected -> Result.success()
            is WallabagSyncResult.Failure -> Result.retry()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WallabagSyncWorkerEntryPoint {
    fun wallabagRepository(): WallabagRepository
}

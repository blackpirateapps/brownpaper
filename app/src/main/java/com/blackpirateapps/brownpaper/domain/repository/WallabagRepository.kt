package com.blackpirateapps.brownpaper.domain.repository

import com.blackpirateapps.brownpaper.data.wallabag.WallabagLoginResult
import com.blackpirateapps.brownpaper.data.wallabag.WallabagSyncResult
import kotlinx.coroutines.flow.Flow

data class WallabagAccountState(
    val isConnected: Boolean = false,
    val host: String = "https://app.wallabag.it",
    val username: String = "",
    val lastSyncAtMillis: Long = 0,
)

interface WallabagRepository {
    val accountState: Flow<WallabagAccountState>

    suspend fun login(
        host: String,
        username: String,
        password: String,
        clientId: String,
        clientSecret: String,
    ): WallabagLoginResult

    suspend fun disconnect()
    suspend fun syncNow(): WallabagSyncResult
    suspend fun syncAnnotationsForArticle(articleId: Long): WallabagSyncResult
    fun scheduleSync()
}
